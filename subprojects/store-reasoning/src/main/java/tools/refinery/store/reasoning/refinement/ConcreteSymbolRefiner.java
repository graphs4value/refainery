/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.refinement;

import tools.refinery.logic.AbstractValue;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Objects;

public class ConcreteSymbolRefiner<A extends AbstractValue<A, C>, C>
		extends AbstractPartialInterpretationRefiner<A, C> {
	private final Interpretation<A> interpretation;

	public ConcreteSymbolRefiner(ReasoningAdapter adapter, PartialSymbol<A, C> partialSymbol,
								 Symbol<A> concreteSymbol) {
		super(adapter, partialSymbol);
		interpretation = adapter.getModel().getInterpretation(concreteSymbol);
	}

	@Override
	public boolean merge(Tuple key, A value) {
		var currentValue = get(key);
		var mergedValue = currentValue.meet(value);
		if (!Objects.equals(currentValue, mergedValue)) {
			put(key, mergedValue);
		}
		return true;
	}

	protected A get(Tuple key) {
		return interpretation.get(key);
	}

	protected A put(Tuple key, A value) {
		return interpretation.put(key, value);
	}

	public static <A1 extends AbstractValue<A1, C1>, C1> Factory<A1, C1> of(Symbol<A1> concreteSymbol) {
		return (adapter, partialSymbol) -> new ConcreteSymbolRefiner<>(adapter, partialSymbol, concreteSymbol);
	}
}
