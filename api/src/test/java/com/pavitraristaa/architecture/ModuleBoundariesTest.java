package com.pavitraristaa.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Keeps the features (packages) of the modular monolith from tangling. Adding a dependency between features is
 * allowed, but it must be a conscious change to {@link #ALLOWED_DEPENDENCIES}.
 */
class ModuleBoundariesTest {

    private static final String BASE = "com.pavitraristaa";

    /** Feature -> the other features it may depend on. Everything else is forbidden. */
    private static final Map<String, Set<String>> ALLOWED_DEPENDENCIES = Map.ofEntries(
            Map.entry("auth", Set.of()),
            Map.entry("master", Set.of()),
            Map.entry("relationship", Set.of("auth")),
            Map.entry("media", Set.of("auth")),
            Map.entry("profile", Set.of("auth", "master", "media", "relationship")),
            Map.entry("preference", Set.of("auth", "master", "profile")),
            Map.entry("trust", Set.of("auth", "profile")),
            Map.entry("favorites", Set.of("auth", "profile", "trust")),
            Map.entry("discovery", Set.of("auth", "profile", "relationship", "trust")),
            Map.entry("connections", Set.of("auth", "master", "preference", "profile", "relationship", "trust")),
            Map.entry("messaging", Set.of("auth", "connections", "media", "profile", "trust")),
            Map.entry("support", Set.of("auth"))
    );

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);

    @Test
    void featuresOnlyDependOnTheFeaturesTheyDeclare() {
        ALLOWED_DEPENDENCIES.forEach((feature, allowed) -> {
            Set<String> forbidden = new TreeSet<>(ALLOWED_DEPENDENCIES.keySet());
            forbidden.remove(feature);
            forbidden.removeAll(allowed);
            noClasses().that().resideInAPackage(packageOf(feature))
                    .should().dependOnClassesThat().resideInAnyPackage(
                            forbidden.stream().map(ModuleBoundariesTest::packageOf).toArray(String[]::new))
                    .because("'" + feature + "' may only depend on " + allowed
                            + "; change ALLOWED_DEPENDENCIES if this dependency is intended")
                    .check(CLASSES);
        });
    }

    @Test
    void featuresAreFreeOfDependencyCycles() {
        slices().matching(BASE + ".(*)..")
                .should().beFreeOfCycles()
                // config is the composition root (SecurityConfig wires auth), so only the feature graph is checked
                .ignoreDependency(alwaysTrue(), resideInAPackage(BASE + ".config.."))
                .check(CLASSES);
    }

    @Test
    void featuresUseConfigOnlyForProperties() {
        noClasses().that().resideInAnyPackage(
                        ALLOWED_DEPENDENCIES.keySet().stream().map(ModuleBoundariesTest::packageOf).toArray(String[]::new))
                .should().dependOnClassesThat(resideInAPackage(BASE + ".config..")
                        .and(not(nameStartingWith(BASE + ".config.PavitraProperties"))))
                .because("config holds wiring; features should only read PavitraProperties")
                .check(CLASSES);
    }

    @Test
    void controllersDoNotExposeEntities() {
        noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..entity..")
                .because("JPA entities must not be exposed from REST controllers; use DTOs")
                .check(CLASSES);
    }

    private static String packageOf(String feature) {
        return BASE + "." + feature + "..";
    }

    private static DescribedPredicate<JavaClass> nameStartingWith(String prefix) {
        return describe("name starts with " + prefix, javaClass -> javaClass.getName().startsWith(prefix));
    }
}
