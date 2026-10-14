package com.sprint.monew.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 패키지 경계 규칙.
 *
 * <ul>
 *   <li>모듈(도메인)끼리는 상대 모듈의 {@code api} 패키지만 참조한다.</li>
 *   <li>{@code domain}은 {@code web}·{@code application}·{@code infra}에 의존하지 않는다.</li>
 *   <li>{@code web}은 {@code infra}에 직접 의존하지 않는다.</li>
 *   <li>모듈 간 순환 의존을 금지한다.</li>
 * </ul>
 */
class ArchitectureTest {

	private static final String ROOT = "com.sprint.monew";
	private static final List<String> MODULES =
		List.of("user", "interest", "article", "comment", "notification", "activity", "backup");
	private static final String[] INTERNAL_LAYERS = {"web", "application", "domain", "infra"};

	private static JavaClasses classes;

	@BeforeAll
	static void importClasses() {
		classes = new ClassFileImporter()
			.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
			.importPackages(ROOT);
	}

	static List<String> modules() {
		return MODULES;
	}

	@ParameterizedTest(name = "{0} 모듈은 다른 모듈의 내부 패키지에 의존하지 않는다")
	@MethodSource("modules")
	void moduleDependsOnlyOnOtherModulesApi(String module) {
		String[] otherInternals = MODULES.stream()
			.filter(other -> !other.equals(module))
			.flatMap(other -> Arrays.stream(INTERNAL_LAYERS)
				.map(layer -> ROOT + "." + other + "." + layer + ".."))
			.toArray(String[]::new);

		noClasses().that().resideInAPackage(ROOT + "." + module + "..")
			.should().dependOnClassesThat().resideInAnyPackage(otherInternals)
			.allowEmptyShould(true)
			.check(classes);
	}

	@Test
	@DisplayName("domain은 web·application·infra에 의존하지 않는다")
	void domainIsIndependent() {
		noClasses().that().resideInAPackage(ROOT + ".*.domain..")
			.should().dependOnClassesThat()
			.resideInAnyPackage(ROOT + ".*.web..", ROOT + ".*.application..", ROOT + ".*.infra..")
			.allowEmptyShould(true)
			.check(classes);
	}

	@Test
	@DisplayName("web은 infra에 직접 의존하지 않는다")
	void webDoesNotUseInfra() {
		noClasses().that().resideInAPackage(ROOT + ".*.web..")
			.should().dependOnClassesThat().resideInAPackage(ROOT + ".*.infra..")
			.allowEmptyShould(true)
			.check(classes);
	}

	@Test
	@DisplayName("모듈 간 순환 의존이 없다")
	void noCyclesBetweenModules() {
		slices().matching(ROOT + ".(*)..")
			.should().beFreeOfCycles()
			.allowEmptyShould(true)
			.check(classes);
	}
}
