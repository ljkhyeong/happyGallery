package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.AdminAuthFilter;
import com.personal.happygallery.adapter.in.web.CustomerAuthFilter;
import com.personal.happygallery.adapter.in.web.GlobalExceptionHandler;
import com.personal.happygallery.adapter.in.web.resolver.AuthUserIdResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
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
            request.setAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR, CUSTOMER_USER_ID);
            return request;
        };
    }

    protected static RequestPostProcessor customerUser(Object user) {
        return request -> {
            request.setAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR, CUSTOMER_USER_ID);
            request.setAttribute(CustomerAuthFilter.CUSTOMER_USER_ATTR, user);
            return request;
        };
    }

    protected static RequestPostProcessor adminUser() {
        return request -> {
            request.setAttribute(AdminAuthFilter.ADMIN_USER_ID_ATTR, ADMIN_USER_ID);
            return request;
        };
    }
}
