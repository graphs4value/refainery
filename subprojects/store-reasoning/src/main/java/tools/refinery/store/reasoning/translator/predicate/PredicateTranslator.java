/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.predicate;

import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.view.ForbiddenView;
import tools.refinery.store.query.view.MayView;
import tools.refinery.store.query.view.MustView;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.representation.Symbol;

import java.util.List;
import java.util.Objects;

import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.store.reasoning.literal.PartialLiterals.may;
import static tools.refinery.store.reasoning.literal.PartialLiterals.must;

public class PredicateTranslator implements ModelStoreConfiguration {
	private final PartialRelation relation;
	private final RelationalQuery query;
	private final boolean mutable;
	private final TruthValue defaultValue;
	private final List<PartialRelation> parameterTypes;

	public PredicateTranslator(PartialRelation relation, RelationalQuery query, List<PartialRelation> parameterTypes,
							   boolean mutable, TruthValue defaultValue) {
		this.parameterTypes = parameterTypes;
		if (relation.arity() != query.arity()) {
			throw new TranslationException(relation, "Expected arity %d query for partial relation %s, got %d instead"
					.formatted(relation.arity(), relation, query.arity()));
		}
		if (defaultValue.must()) {
			throw new TranslationException(relation, "Default value must be UNKNOWN or FALSE");
		}
		this.relation = relation;
		this.query = query;
		this.mutable = mutable;
		this.defaultValue = defaultValue;
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		var translator = PartialRelationTranslator.of(relation)
				.query(query);
		if (mutable) {
			var symbol = Symbol.of(relation.name(), relation.arity(), TruthValue.class, defaultValue);
			translator.symbol(symbol);

			var parameters = new NodeVariable[relation.arity()];
			for (int i = 0; i < parameters.length; i++) {
				parameters[i] = Variable.of("p" + i);
			}

			var must = Query.builder()
					.parameters(parameters)
					.clause(must(query.call(parameters)))
					.clause(new MustView(symbol).call(parameters))
					.build();
			translator.must(must);

			var mayLiterals = new Literal[2];
			mayLiterals[0] = may(query.call(parameters));
			if (defaultValue.may()) {
				mayLiterals[1] = not(new ForbiddenView(symbol).call(parameters));
			} else {
				mayLiterals[1] = new MayView(symbol).call(parameters);
			}
			var may = Query.builder()
					.parameters(parameters)
					.clause(mayLiterals)
					.build();
			translator.may(may);

			if (parameterTypes != null && parameterTypes.stream().anyMatch(Objects::nonNull)) {
				translator.refiner(PredicateRefiner.of(symbol, parameterTypes));
			}
		} else if (defaultValue.may()) {
			// If all values are permitted, we don't need to check for any forbidden values in the model.
			// If the result of this predicate of {@code ERROR}, some other partial relation (that we check for)
			// will be {@code ERROR} as well.
			translator.exclude(null);
			translator.accept(null);
			translator.objective(null);
		} else {
			translator.mayNever();
		}
		storeBuilder.with(translator);
	}
}
