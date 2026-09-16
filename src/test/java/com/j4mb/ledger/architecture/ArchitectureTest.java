package com.j4mb.ledger.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.j4mb.ledger")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainMustNotDependOnInfrastructure =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .because("Domain layer must not depend on infrastructure");

    @ArchTest
    static final ArchRule domainMustNotDependOnSpringWeb =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework.web..")
                    .because("Domain entities must remain framework-independent");

    @ArchTest
    static final ArchRule repositoriesMustNotBeCalledFromDomain =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
                    .because("Domain objects must not access repositories directly");

    @ArchTest
    static final ArchRule noLombok =
            noClasses().should().accessClassesThat().haveNameMatching(".*lombok.*")
                    .because("Lombok is banned — use records or explicit constructors");

    @ArchTest
    static final ArchRule controllersMustNotCallRepositories =
            noClasses().that().haveSimpleNameEndingWith("Controller")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
                    .because("Controllers must delegate to services, not repositories");
}
