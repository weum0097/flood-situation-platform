package com.example.flood.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

@AnalyzeClasses(packages = "com.example.flood", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundaryTest {

    @ArchTest
    static final ArchRule api_does_not_access_persistence =
        noClasses().that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_stays_independent =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "com.example.flood.event.api..",
                "com.example.flood.region.api..",
                "com.example.flood.situation.api..",
                "com.example.flood.material.api..",
                "com.example.flood.security.api..",
                "..infrastructure..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule modules_have_no_cycles =
        SlicesRuleDefinition.slices()
            .matching("com.example.flood.(*)..")
            .should().beFreeOfCycles();
}
