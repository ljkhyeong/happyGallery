package com.personal.happygallery.adapter.in.web.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@SecurityScheme(
        name = OpenApiSecuritySchemes.CUSTOMER_SESSION,
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.COOKIE,
        paramName = "HG_SESSION")
@SecurityScheme(
        name = OpenApiSecuritySchemes.ADMIN_BEARER,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "opaque")
@SecurityScheme(
        name = OpenApiSecuritySchemes.ADMIN_API_KEY,
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-Admin-Key")
public class OpenApiSecuritySchemes {

    public static final String CUSTOMER_SESSION = "CustomerSession";
    public static final String ADMIN_BEARER = "AdminBearer";
    public static final String ADMIN_API_KEY = "AdminApiKey";

    private OpenApiSecuritySchemes() {}
}
