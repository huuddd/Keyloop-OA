package com.keyloop.scheduler.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.keyloop.scheduler");
    }

    // ============================
    // Layer dependency rules
    // ============================

    @Nested
    @DisplayName("Layer dependency rules")
    class LayerDependencyRules {

        @Test
        @DisplayName("Domain must not depend on application, api, or infrastructure")
        void domainShouldNotDependOnOtherLayers() {
            noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                    "..application..", "..api..", "..infrastructure..")
                .check(classes);
        }

        @Test
        @DisplayName("Application must not depend on infrastructure or api")
        void applicationShouldNotDependOnInfrastructureOrApi() {
            noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                    "..infrastructure..", "..api..")
                .check(classes);
        }

        @Test
        @DisplayName("Infrastructure must not depend on api")
        void infrastructureShouldNotDependOnApi() {
            noClasses().that().resideInAPackage("..infrastructure..")
                .should().dependOnClassesThat().resideInAnyPackage(
                    "..api..")
                .check(classes);
        }

        @Test
        @DisplayName("Api must not be imported by any other layer")
        void apiShouldNotBeImportedByOtherLayers() {
            noClasses().that().resideInAnyPackage(
                    "..domain..", "..application..", "..infrastructure..")
                .should().dependOnClassesThat().resideInAPackage("..api..")
                .check(classes);
        }
    }

    // ============================
    // Framework isolation rules
    // ============================

    @Nested
    @DisplayName("Framework isolation rules")
    class FrameworkIsolationRules {

        @Test
        @DisplayName("Domain must not use Spring annotations")
        void domainShouldNotUseSpringAnnotations() {
            noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..")
                .check(classes);
        }

        @Test
        @DisplayName("Domain must not use JPA annotations")
        void domainShouldNotUseJpaAnnotations() {
            noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..")
                .check(classes);
        }
    }

    // ============================
    // Naming conventions
    // ============================

    @Nested
    @DisplayName("Naming conventions")
    class NamingConventions {

        @Test
        @DisplayName("Controllers must end with 'Controller'")
        void controllersShouldEndWithController() {
            classes().that().resideInAPackage("..api.controller..")
                .should().haveSimpleNameEndingWith("Controller")
                .check(classes);
        }

        @Test
        @DisplayName("JPA entities must end with 'JpaEntity'")
        void jpaEntitiesShouldEndWithJpaEntity() {
            classes().that().resideInAPackage("..infrastructure.persistence.entity..")
                .and().areAnnotatedWith(jakarta.persistence.Entity.class)
                .should().haveSimpleNameEndingWith("JpaEntity")
                .check(classes);
        }

        @Test
        @DisplayName("Repository interfaces must reside in domain packages")
        void repositoryInterfacesShouldBeInDomain() {
            classes().that().haveSimpleNameEndingWith("Repository")
                .and().areInterfaces()
                .and().resideInAPackage("..com.keyloop.scheduler..")
                // Exclude Spring Data marker interfaces in infrastructure
                .and().resideOutsideOfPackage("..infrastructure..")
                .should().resideInAPackage("..domain..")
                .check(classes);
        }
    }
}
