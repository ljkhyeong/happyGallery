package com.personal.happygallery.adapter.out.persistence.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.mysql.MySQLContainer;

@Tag("usecase")
class JdbcImageMediaReferenceReaderAdapterUseCaseIT {

    @DisplayName("테이블 비교 규칙이 달라도 모든 이미지 참조와 대소문자가 다른 파일을 보존한다")
    @Test
    void findReferencedImageUrls_withMixedCollations() {
        try (MySQLContainer mysql = new MySQLContainer("mysql:8.0")) {
            mysql.start();
            JdbcClient jdbc = JdbcClient.create(new DriverManagerDataSource(
                    mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()));
            List<String> tables = List.of(
                    "products", "classes", "events", "review_images", "review_evidence_snapshot_images");
            List<String> collations = List.of(
                    "utf8mb4_general_ci", "utf8mb4_unicode_ci", "utf8mb4_0900_ai_ci",
                    "utf8mb4_unicode_ci", "utf8mb4_general_ci");
            List<String> urls = List.of(
                    "/api/v1/media/images/product.png", "/api/v1/media/images/class.png",
                    "/api/v1/media/images/event.png", "/api/v1/media/images/리뷰.png",
                    "/api/v1/media/images/evidence.png");
            for (int index = 0; index < tables.size(); index++) {
                jdbc.sql("CREATE TABLE " + tables.get(index)
                        + " (image_url VARCHAR(512) CHARACTER SET utf8mb4 COLLATE "
                        + collations.get(index) + ")").update();
                jdbc.sql("INSERT INTO " + tables.get(index) + " (image_url) VALUES (?)")
                        .param(urls.get(index)).update();
            }
            jdbc.sql("INSERT INTO products (image_url) VALUES (NULL), (?), (?)")
                    .param("/api/v1/media/images/File.png").param(urls.getFirst()).update();
            jdbc.sql("INSERT INTO classes (image_url) VALUES (NULL), (?)")
                    .param("/api/v1/media/images/file.png").update();
            jdbc.sql("INSERT INTO events (image_url) VALUES (NULL)").update();

            List<String> references = new JdbcImageMediaReferenceReaderAdapter(jdbc).findReferencedImageUrls();

            assertThat(references).containsExactlyInAnyOrderElementsOf(List.of(
                    urls.get(0), urls.get(1), urls.get(2), urls.get(3), urls.get(4),
                    "/api/v1/media/images/File.png", "/api/v1/media/images/file.png"));
        }
    }
}
