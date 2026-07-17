package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.GlobalExceptionHandler;
import com.personal.happygallery.adapter.in.web.resolver.AuthUserIdResolver;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;

@Tag("restdocs")
@ExtendWith(RestDocumentationExtension.class)
abstract class RestDocsTestSupport {

    protected static final Long CUSTOMER_USER_ID = 11L;
    protected static final Long ADMIN_USER_ID = 99L;

    protected MockMvc mockMvc(RestDocumentationContextProvider restDocumentation, Object... controllers) {
        return MockMvcBuilders.standaloneSetup(controllers)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthUserIdResolver())
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

    protected static String json(String body) {
        return body;
    }

    protected static MediaType jsonContent() {
        return MediaType.APPLICATION_JSON;
    }

    protected static RequestPostProcessor customerUser() {
        return request -> {
            request.setUserPrincipal(new TestingAuthenticationToken(
                    new CustomerPrincipal(
                            CUSTOMER_USER_ID,
                            "member@example.com",
                            "회원",
                            "01012345678",
                            true
                    ),
                    null,
                    "ROLE_CUSTOMER"));
            return request;
        };
    }

    protected static RequestPostProcessor adminUser() {
        return request -> {
            request.setUserPrincipal(new TestingAuthenticationToken(
                    AdminPrincipal.bearerSession(ADMIN_USER_ID, "admin"), null, "ROLE_ADMIN"));
            return request;
        };
    }
}
