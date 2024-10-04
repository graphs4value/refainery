/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests.validation;

import com.google.inject.Inject;
import org.eclipse.emf.common.util.Diagnostic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.refinery.language.tests.InjectWithRefinery;
import tools.refinery.language.tests.utils.ProblemParseHelper;
import tools.refinery.language.validation.ProblemValidator;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@InjectWithRefinery
class SubsetValidationTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@ParameterizedTest
	@ValueSource(strings = {"""
			class Foo {
				Foo[] bar
				Foo[] bar2 subsets bar
			}
			""", """
			class Foo {
				Foo[] bar
				Foo[] bar2
				Foo[] bar3 subsets bar, bar2
			}
			""", """
			class Foo {
				Foo[] bar
			}

			class Foo2 extends Foo {
				Foo[] bar2 subsets bar
			}
			""", """
			class Foo {
				Foo[] bar
			}

			class Foo2 {
				% Valid, but only satisfiable if Foo and Foo2 have a common subclass.
				Foo[] bar2 subsets bar
			}
			""", """
			pred bar(Foo a, Foo b).

			class Foo {
				Foo[] bar2 subsets bar
			}
			""", """
			pred bar(Foo a, Foo b) <-> !bar2(a, b).

			class Foo {
				Foo[] bar2 subsets bar
			}
			"""})
	void validSubsetTest(String text) {
		var problem = parseHelper.parse(text);
		var issues = problem.validate();
		assertThat(issues, not(hasItems(hasProperty("issueCode", in(Set.of(
				ProblemValidator.INVALID_ARITY_ISSUE,
				ProblemValidator.INVALID_SUPERSET_ISSUE
		))))));
	}

	@ParameterizedTest
	@ValueSource(strings = {"""
			class bar.

			class Foo {
				Foo[] bar2 subsets bar
			}
			""", """
			pred bar(Foo a).

			class Foo {
				Foo[] bar2 subsets bar
			}
			""", """
			pred bar(Foo a, Foo b, Foo c).

			class Foo {
				Foo[] bar2 subsets bar
			}
			"""})
	void invalidSubsetArityTest(String text) {
		var problem = parseHelper.parse(text);
		var issues = problem.validate();
		assertThat(issues, hasItem(allOf(
				hasProperty("severity", is(Diagnostic.ERROR)),
				hasProperty("issueCode", is(ProblemValidator.INVALID_ARITY_ISSUE)),
				hasProperty("message", stringContainsInOrder("bar", "bar2"))
		)));
	}

	@Test
	void subsetDatatypeTest() {
		var problem = parseHelper.parse("""
				class Foo {
					Foo[] bar subsets string
				}
				""");
		var issues = problem.validate();
		assertThat(issues, hasItem(allOf(
				hasProperty("severity", is(Diagnostic.ERROR)),
				hasProperty("issueCode", is(ProblemValidator.INVALID_SUPERSET_ISSUE)),
				hasProperty("message", stringContainsInOrder("bar", "datatype"))
		)));
	}

	@Test
	void subsetSelfTest() {
		var problem = parseHelper.parse("""
				class Foo {
					Foo[] bar subsets bar
				}
				""");
		var issues = problem.validate();
		assertThat(issues, hasItem(allOf(
				hasProperty("severity", is(Diagnostic.ERROR)),
				hasProperty("issueCode", is(ProblemValidator.INVALID_SUPERSET_ISSUE)),
				hasProperty("message", stringContainsInOrder("bar", "itself"))
		)));
	}
}
