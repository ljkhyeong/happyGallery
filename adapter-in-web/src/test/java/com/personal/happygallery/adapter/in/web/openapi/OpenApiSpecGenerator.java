package com.personal.happygallery.adapter.in.web.openapi;

import com.personal.happygallery.support.UseCaseIT;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("openapi")
@UseCaseIT
@Import(OpenApiSpecGenerator.OpenApiConfiguration.class)
@TestPropertySource(properties = {
        "springdoc.api-docs.enabled=true",
        "springdoc.paths-to-match=/api/v1/**"
})
class OpenApiSpecGenerator {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Controller와 DTO에서 OpenAPI 명세를 생성한다")
    void generateOpenApi() throws Exception {
        String openApi = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        String canonicalOpenApi = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(sortObjectKeys(objectMapper.readValue(openApi, Object.class))) + "\n";
        Path output = Path.of(System.getProperty("openapi.output"));
        Files.createDirectories(output.getParent());
        Files.writeString(output, canonicalOpenApi, StandardCharsets.UTF_8);
    }

    private Object sortObjectKeys(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            map.forEach((key, child) -> sorted.put(String.valueOf(key), sortObjectKeys(child)));
            return sorted;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::sortObjectKeys).toList();
        }
        return value;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class OpenApiConfiguration {

        @Bean
        OpenAPI happyGalleryOpenApi() {
            return new OpenAPI()
                    .info(new Info().title("happyGallery API").version("v1"))
                    .servers(List.of(new Server().url("/").description("Same-origin API")));
        }
    }
}
