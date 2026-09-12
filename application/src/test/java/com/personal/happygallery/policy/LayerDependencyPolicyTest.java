package com.personal.happygallery.policy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 운영 클래스의 실제 의존 관계를 검사한다. 테스트 fixture의 역방향 의존은 제외한다.
 *
 * <p>의존 방향: bootstrap → adapter-in-web/out-* → application → domain
 *
 * <ul>
 *   <li>domain 은 다른 모듈을 import 하지 않는다</li>
 *   <li>application 은 adapter.* / bootstrap 을 import 하지 않는다</li>
 *   <li>adapter.in.web 은 adapter.out.* 을 직접 import 하지 않는다 (port.in 을 통해서만 호출)</li>
 *   <li>adapter.in.web 은 application.*.port.out 도 직접 import 하지 않는다</li>
 *   <li>adapter.out.persistence ↔ adapter.out.external 은 서로 import 하지 않는다</li>
 *   <li>adapter.out.* 은 adapter.in.web 을 import 하지 않는다</li>
 * </ul>
 */
@Tag("architecture")
class LayerDependencyPolicyTest {

    private static final String ROOT = "com.personal.happygallery";
    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    @DisplayName("웹·서비스·도메인은 Spring Data 저장소를 직접 사용하지 않는다")
    @Test
    void inner_layers_should_not_depend_on_spring_data_repositories() {
        noClasses()
                .that().resideInAnyPackage("..adapter.in.web..", "..application..", "..domain..")
                .should().dependOnClassesThat().areAssignableTo(Repository.class)
                .check(classes);
    }

    @DisplayName("adapter 는 실행 모듈 bootstrap 에 의존하지 않는다")
    @Test
    void adapters_should_not_depend_on_bootstrap() {
        noClasses()
                .that().resideInAPackage("..adapter..")
                .should().dependOnClassesThat().resideInAPackage("..bootstrap..")
                .check(classes);
    }

    // ── domain 보호 ──

    @DisplayName("domain 은 application 을 import 하지 않는다")
    @Test
    void domain_should_not_depend_on_application() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..application..")
                .check(classes);
    }

    @DisplayName("domain 은 adapter.* 를 import 하지 않는다")
    @Test
    void domain_should_not_depend_on_adapter() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..adapter..")
                .check(classes);
    }

    @DisplayName("domain 은 bootstrap 을 import 하지 않는다")
    @Test
    void domain_should_not_depend_on_bootstrap() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..bootstrap..")
                .check(classes);
    }

    // ── application 보호 ──

    @DisplayName("application 은 adapter.* 를 import 하지 않는다")
    @Test
    void application_should_not_depend_on_adapter() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..adapter..")
                .check(classes);
    }

    @DisplayName("application 은 bootstrap 을 import 하지 않는다")
    @Test
    void application_should_not_depend_on_bootstrap() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..bootstrap..")
                .check(classes);
    }

    // ── adapter.in.web 경계 ──

    @DisplayName("adapter.in.web 은 adapter.out.* 를 직접 import 하지 않는다 (port.in 을 통해서만 호출)")
    @Test
    void adapter_in_web_should_not_depend_on_adapter_out() {
        noClasses()
                .that().resideInAPackage("..adapter.in.web..")
                .should().dependOnClassesThat().resideInAPackage("..adapter.out..")
                .check(classes);
    }

    @DisplayName("adapter.in.web 은 application.*.port.out 을 직접 import 하지 않는다")
    @Test
    void adapter_in_web_should_not_depend_on_port_out() {
        noClasses()
                .that().resideInAPackage("..adapter.in.web..")
                .should().dependOnClassesThat().resideInAPackage("..port.out..")
                .check(classes);
    }

    // ── adapter.out.* 경계 ──

    @DisplayName("adapter.out.persistence 는 adapter.in.web / adapter.out.external 을 import 하지 않는다")
    @Test
    void adapter_out_persistence_should_not_depend_on_other_adapters() {
        noClasses()
                .that().resideInAPackage("..adapter.out.persistence..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..adapter.in.web..",
                        "..adapter.out.external.."
                )
                .check(classes);
    }

    @DisplayName("adapter.out.external 은 adapter.in.web / adapter.out.persistence 를 import 하지 않는다")
    @Test
    void adapter_out_external_should_not_depend_on_other_adapters() {
        noClasses()
                .that().resideInAPackage("..adapter.out.external..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..adapter.in.web..",
                        "..adapter.out.persistence.."
                )
                .check(classes);
    }
}
