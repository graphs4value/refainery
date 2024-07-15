/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.term.StatefulAggregate;
import tools.refinery.logic.term.StatefulAggregator;
import tools.refinery.logic.term.Variable;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.FunctionView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static tools.refinery.store.query.interpreter.tests.QueryAssertions.assertNullableResults;

class AggregatorBatchingTest {
	private static final Symbol<Boolean> person = Symbol.of("Person", 1);
	private static final Symbol<Integer> values = Symbol.of("values", 2, Integer.class, null);
	private static final AnySymbolView personView = new KeyOnlyView<>(person);
	private static final FunctionView<Integer> valuesView = new FunctionView<>(values);

	private final Query<Integer> query = Query.of(Integer.class, (builder, p1, output) -> builder
			.clause(
					personView.call(p1),
					output.assign(valuesView.aggregate(new InstrumentedAggregator(), p1, Variable.of()))
			));
	private final Query<Integer> queryMax = Query.of(Integer.class, (builder, p1, output) -> builder
			.clause(
					personView.call(p1),
					output.assign(valuesView.aggregate(new InstrumentedAggregatorMax(), p1, Variable.of()))
			));

	private int extractCount = 0;

	@Test
	void batchTest() {
		var model = createModel();
		var personInterpretation = model.getInterpretation(person);
		var valuesInterpretation = model.getInterpretation(values);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var resultSet = queryEngine.getResultSet(query);
		var resultSetMax = queryEngine.getResultSet(queryMax);

		assertThat(extractCount, is(1));

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);

		valuesInterpretation.put(Tuple.of(0, 0), 1);
		valuesInterpretation.put(Tuple.of(0, 1), 2);
		valuesInterpretation.put(Tuple.of(0, 2), 3);
		valuesInterpretation.put(Tuple.of(1, 0), 1);
		valuesInterpretation.put(Tuple.of(1, 1), -1);

		queryEngine.flushChanges();

		assertThat(extractCount, is(5));

		assertNullableResults(Map.of(
				Tuple.of(0), Optional.of(6),
				Tuple.of(1), Optional.of(0),
				Tuple.of(2), Optional.empty()
		), resultSet);
		assertNullableResults(Map.of(
				Tuple.of(0), Optional.of(3),
				Tuple.of(1), Optional.of(1),
				Tuple.of(2), Optional.empty()
		), resultSetMax);
	}

	@Test
	void separateTest() {
		var model = createModel();
		var personInterpretation = model.getInterpretation(person);
		var valuesInterpretation = model.getInterpretation(values);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var resultSet = queryEngine.getResultSet(query);

		assertThat(extractCount, is(1));

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);

		queryEngine.flushChanges();
		assertThat(extractCount, is(3));

		valuesInterpretation.put(Tuple.of(0, 0), 1);
		valuesInterpretation.put(Tuple.of(1, 0), 1);

		queryEngine.flushChanges();
		assertThat(extractCount, is(5));
		assertNullableResults(Map.of(
				Tuple.of(0), Optional.of(1),
				Tuple.of(1), Optional.of(1),
				Tuple.of(2), Optional.empty()
		), resultSet);

		valuesInterpretation.put(Tuple.of(0, 1), 2);
		valuesInterpretation.put(Tuple.of(1, 1), -1);

		queryEngine.flushChanges();
		assertThat(extractCount, is(9));
		assertNullableResults(Map.of(
				Tuple.of(0), Optional.of(3),
				Tuple.of(1), Optional.of(0),
				Tuple.of(2), Optional.empty()
		), resultSet);

		valuesInterpretation.put(Tuple.of(0, 2), 3);

		queryEngine.flushChanges();
		assertThat(extractCount, is(11));
		assertNullableResults(Map.of(
				Tuple.of(0), Optional.of(6),
				Tuple.of(1), Optional.of(0),
				Tuple.of(2), Optional.empty()
		), resultSet);
	}

	private Model createModel() {
		var store = ModelStore.builder()
				.symbols(person, values)
				.with(QueryInterpreterAdapter.builder()
						.queries(query, queryMax))
				.build();
		return store.createEmptyModel();
	}

	class InstrumentedAggregator implements StatefulAggregator<Integer, Integer> {
		@Override
		public Class<Integer> getResultType() {
			return Integer.class;
		}

		@Override
		public Class<Integer> getInputType() {
			return Integer.class;
		}

		@Override
		public StatefulAggregate<Integer, Integer> createEmptyAggregate() {
			return new InstrumentedAggregate();
		}
	}

	class InstrumentedAggregate implements StatefulAggregate<Integer, Integer> {
		private int sum;

		public InstrumentedAggregate() {
			this(0);
		}

		private InstrumentedAggregate(int sum) {
			this.sum = sum;
		}


		@Override
		public void add(Integer value) {
			sum += value;
		}

		@Override
		public void remove(Integer value) {
			sum -= value;
		}

		@NotNull
		@Override
		public Integer getResult() {
			extractCount++;
			return sum;
		}

		@Override
		public boolean isEmpty() {
			return sum == 0;
		}

		@Override
		public StatefulAggregate<Integer, Integer> deepCopy() {
			return new InstrumentedAggregate(sum);
		}
	}

	class InstrumentedAggregatorMax implements StatefulAggregator<Integer, Integer> {
		@Override
		public Class<Integer> getResultType() {
			return Integer.class;
		}

		@Override
		public Class<Integer> getInputType() {
			return Integer.class;
		}

		@Override
		public StatefulAggregate<Integer, Integer> createEmptyAggregate() {
			return new InstrumentedAggregateMax();
		}
	}
	class InstrumentedAggregateMax implements StatefulAggregate<Integer, Integer> {
		private final List<Integer> numbers;

		public InstrumentedAggregateMax() {
			this.numbers = new ArrayList<>();
		}
		public InstrumentedAggregateMax(List<Integer> numbers) {
			this.numbers = new ArrayList<>();
			this.numbers.addAll(numbers);
		}

		@Override
		public void add(Integer value) {
			numbers.add(value);
		}

		@Override
		public void remove(Integer value) {
			numbers.remove(value);
		}

		@Override
		public Integer getResult() {
			if(numbers.isEmpty()){
				return null;
			} else {
				return Collections.max(numbers);
			}
		}

		@Override
		public boolean isEmpty() {
			return numbers.isEmpty();
		}

		@Override
		public StatefulAggregate<Integer, Integer> deepCopy() {
			return new InstrumentedAggregateMax(numbers);
		}
	}
}
