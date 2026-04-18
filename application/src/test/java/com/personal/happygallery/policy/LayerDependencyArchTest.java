package com.personal.happygallery.policy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * [PolicyTest] 레이어 의존 규칙 — 모듈 분리 없이 패키지 수준에서 강제한다.
 *
 * <ul>
 *   <li>domain 패키지는 app·infra·config 패키지를 참조하지 않는다</li>
 *   <li>web(Controller) 패키지는 port.out(저장소 포트)에 직접 의존하지 않는다</li>
 *   <li>infra 패키지는 port.out 인터페이스만 구현하며, 유스케이스 서비스를 참조하지 않는다</li>
 * </ul>
 */
@Tag("policy")
class LayerDependencyArchTest {

    private static final String ROOT = "com.personal.happygallery";
    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    // ── domain 레이어 보호 ──

    @DisplayName("domain 패키지는 app 패키지를 참조하지 않는다")
    @Test
    void domain_should_not_depend_on_app() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..app..")
                .check(classes);
    }

    @DisplayName("domain 패키지는 infra 패키지를 참조하지 않는다")
    @Test
    void domain_should_not_depend_on_infra() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..infra..")
                .check(classes);
    }

    @DisplayName("domain 패키지는 config 패키지를 참조하지 않는다")
    @Test
    void domain_should_not_depend_on_config() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..config..")
                .check(classes);
    }

    // ── web → port.out 차단 ──

    @DisplayName("web 패키지는 port.out(저장소 포트)에 직접 의존하지 않는다")
    @Test
    void web_should_not_access_port_out() {
        noClasses()
                .that().resideInAPackage("..web..")
                .should().dependOnClassesThat().resideInAPackage("..port.out..")
                .check(classes);
    }

    // ── infra → 유스케이스 서비스 차단 ──

    @DisplayName("infra 패키지는 app 하위 서비스 클래스에 의존하지 않는다 (port만 허용)")
    @Test
    void infra_should_only_depend_on_ports_not_services() {
        classes()
                .that().resideInAPackage("..infra..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..infra..",
                        "..domain..",
                        "..port..",
                        "..dashboard.dto..",
                        "..search.dto..",
                        "..product..",       // ProductFilter
                        "java..",
                        "jakarta..",
                        "javax..",
                        "org.springframework..",
                        "org.mybatis..",
                        "org.apache..",
                        "io.github.resilience4j..",
                        "io.micrometer..",
                        "com.fasterxml..",
                        "tools.jackson..",   // Jackson 3.x
                        "org.slf4j.."
                )
                .check(classes);
    }
}
