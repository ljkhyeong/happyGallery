package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.GlobalExceptionHandler;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

@Tag("restdocs")
@ExtendWith(RestDocumentationExtension.class)
abstract class RestDocsTestSupport {

    protected static final Long CUSTOMER_USER_ID = 11L;
    protected static final Long ADMIN_USER_ID = 99L;

    protected MockMvc mockMvc(RestDocumentationContextProvider restDocumentation, Object... controllers) {
        return MockMvcBuilders.standaloneSetup(controllers)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .addFilters(new SecurityContextHolderFilter(
                        new HttpSessionSecurityContextRepository()))
                .apply(documentationConfiguration(restDocumentation)
                        .uris()
                        .withScheme("https")
                        .withHost("api.happygallery.local")
                        .withPort(443)
                        .and()
                        .operationPreprocessors()
                        .withRequestDefaults(prettyPrint())
                        .withResponseDefaults(prettyPrint()))
                .alwaysDo(document("{class-name}/{method-name}"))
                .build();
    }

    protected static RequestPostProcessor customerUser() {
        return authentication(new TestingAuthenticationToken(
                new CustomerPrincipal(
                        CUSTOMER_USER_ID,
                        "member@example.com",
                        "회원",
                        "01012345678",
                        true
                ),
                null,
                "ROLE_CUSTOMER"));
    }

    protected static RequestPostProcessor adminUser() {
        return authentication(new TestingAuthenticationToken(
                AdminPrincipal.bearerSession(ADMIN_USER_ID, "admin"), null, "ROLE_ADMIN"));
    }
}
