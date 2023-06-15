/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.tests;

import org.junit.jupiter.api.Test;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.term.ParameterDirection;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tools.refinery.store.query.tests.QueryMatchers.structurallyEqualTo;

class StructurallyEqualToTest {
	@Test
	void flatEqualsTest() {
		var p = Variable.of("p");
		var q = Variable.of("q");
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var personView = new KeyOnlyView<>(person);

		var expected = Dnf.builder("Expected").parameters(q).clause(personView.call(q)).build();
		var actual = Dnf.builder("Actual").parameters(p).clause(personView.call(p)).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void flatNotEqualsTest() {
		var p = Variable.of("p");
		var q = Variable.of("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyView<>(friend);

		var expected = Dnf.builder("Expected").parameters(q).clause(friendView.call(q, q)).build();
		var actual = Dnf.builder("Actual").parameters(p).clause(friendView.call(p, q)).build();

		var assertion = structurallyEqualTo(expected);
		assertThrows(AssertionError.class, () -> assertThat(actual, assertion));
	}

	@Test
	void deepEqualsTest() {
		var p = Variable.of("p");
		var q = Variable.of("q");
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var personView = new KeyOnlyView<>(person);

		var expected = Dnf.builder("Expected").parameters(q).clause(
				Dnf.builder("Expected2").parameters(p).clause(personView.call(p)).build().call(q)
		).build();
		var actual = Dnf.builder("Actual").parameters(q).clause(
				Dnf.builder("Actual2").parameters(p).clause(personView.call(p)).build().call(q)
		).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void deepNotEqualsTest() {
		var p = Variable.of("p");
		var q = Variable.of("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyView<>(friend);

		var expected = Dnf.builder("Expected").parameters(q).clause(
				Dnf.builder("Expected2").parameters(p).clause(friendView.call(p, p)).build().call(q)
		).build();
		var actual = Dnf.builder("Actual").parameter(q).clause(
				Dnf.builder("Actual2").parameters(p).clause(friendView.call(p, q)).build().call(q)
		).build();

		var assertion = structurallyEqualTo(expected);
		var error = assertThrows(AssertionError.class, () -> assertThat(actual, assertion));
		assertThat(error.getMessage(), containsString(" called from Expected/1 "));
	}

	@Test
	void parameterListLengthMismatchTest() {
		var p = Variable.of("p");
		var q = Variable.of("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyView<>(friend);

		var expected = Dnf.builder("Expected").parameter(p).clause(
				friendView.call(p, p)
		).build();
		var actual = Dnf.builder("Actual").parameters(p, q).clause(
				friendView.call(p, q)
		).build();

		var assertion = structurallyEqualTo(expected);
		assertThrows(AssertionError.class, () -> assertThat(actual, assertion));
	}

	@Test
	void parameterDirectionMismatchTest() {
		var p = Variable.of("p");
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var personView = new KeyOnlyView<>(person);

		var expected = Dnf.builder("Expected").parameter(p, ParameterDirection.OUT).clause(
				personView.call(p)
		).build();
		var actual = Dnf.builder("Actual").parameter(p, ParameterDirection.IN).clause(
				personView.call(p)
		).build();

		var assertion = structurallyEqualTo(expected);
		assertThrows(AssertionError.class, () -> assertThat(actual, assertion));
	}

	@Test
	void clauseCountMismatchTest() {
		var p = Variable.of("p");
		var q = Variable.of("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyView<>(friend);

		var expected = Dnf.builder("Expected")
				.parameters(p, q)
				.clause(friendView.call(p, q))
				.clause(friendView.call(q, p))
				.build();
		var actual = Dnf.builder("Actual").parameters(p, q).clause(
				friendView.call(p, q)
		).build();

		var assertion = structurallyEqualTo(expected);
		assertThrows(AssertionError.class, () -> assertThat(actual, assertion));
	}

	@Test
	void literalCountMismatchTest() {
		var p = Variable.of("p");
		var q = Variable.of("q");
		var friend = new Symbol<>("friend", 2, Boolean.class, false);
		var friendView = new KeyOnlyView<>(friend);

		var expected = Dnf.builder("Expected").parameters(p, q).clause(
				friendView.call(p, q),
				friendView.call(q, p)
		).build();
		var actual = Dnf.builder("Actual").parameters(p, q).clause(
				friendView.call(p, q)
		).build();

		var assertion = structurallyEqualTo(expected);
		assertThrows(AssertionError.class, () -> assertThat(actual, assertion));
	}
}
