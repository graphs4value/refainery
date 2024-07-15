/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.scope;

import tools.refinery.logic.dnf.AnyQuery;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.Variable;
import tools.refinery.store.dse.transition.DesignSpaceExplorationBuilder;
import tools.refinery.store.dse.transition.objectives.Criteria;
import tools.refinery.store.dse.transition.objectives.Objectives;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.literal.CountCandidateUpperBoundLiteral;
import tools.refinery.store.reasoning.literal.CountLowerBoundLiteral;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.Collection;
import java.util.List;

import static tools.refinery.logic.literal.Literals.check;
import static tools.refinery.logic.term.int_.IntTerms.*;
import static tools.refinery.store.reasoning.literal.PartialLiterals.must;
import static tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator.MULTI_VIEW;

class UpperTypeScopePropagator extends TypeScopePropagator {
	private final int upperBound;

	private UpperTypeScopePropagator(BoundScopePropagator adapter, int upperBound, RelationalQuery allQuery,
									 RelationalQuery multiQuery, PartialRelation type) {
		super(adapter, allQuery, multiQuery, type);
		this.upperBound = upperBound;
	}

	@Override
	protected void doUpdateBounds() {
		constraint.setUb((upperBound - getSingleCount()));
	}

	@Override
	public String getName() {
		return "upper type scope bound for '%s'".formatted(getType().name());
	}

	static class Factory extends TypeScopePropagator.Factory {
		private final PartialRelation type;
		private final int upperBound;
		private final RelationalQuery allMust;
		private final RelationalQuery multiMust;

		public Factory(PartialRelation type, int upperBound) {
			this.type = type;
			this.upperBound = upperBound;
			allMust = Query.of(type.name() + "#must", (builder, instance) -> builder.clause(
					must(type.call(instance))
			));
			multiMust = Query.of(type.name() + "#multiMust", (builder, instance) -> builder.clause(
					must(type.call(instance)),
					MULTI_VIEW.call(instance)
			));
		}

		@Override
		public TypeScopePropagator createPropagator(BoundScopePropagator adapter) {
			return new UpperTypeScopePropagator(adapter, upperBound, allMust, multiMust, type);
		}

		@Override
		protected Collection<AnyQuery> getQueries() {
			return List.of(allMust, multiMust);
		}

		@Override
		public void configure(ModelStoreBuilder storeBuilder) {
			super.configure(storeBuilder);

			var excessObjects = Query.of(type.name() + "#excess", Integer.class, (builder, output) -> builder
					.clause(Integer.class, candidateUpperBound -> List.of(
							new CountCandidateUpperBoundLiteral(candidateUpperBound, type, List.of(Variable.of())),
							output.assign(sub(candidateUpperBound, constant(upperBound))),
							check(greater(output, constant(0)))
					)));
			var tooManyObjects = Query.of(type.name() + "#tooMany", builder -> builder
					.clause(Integer.class, lowerBound -> List.of(
							new CountLowerBoundLiteral(lowerBound, type, List.of(Variable.of())),
							check(greater(lowerBound, constant(upperBound)))
					)));

			storeBuilder.getAdapter(ReasoningBuilder.class).objective(Objectives.value(excessObjects));
			storeBuilder.tryGetAdapter(DesignSpaceExplorationBuilder.class).ifPresent(dseBuilder -> {
				dseBuilder.accept(Criteria.whenNoMatch(excessObjects));
				dseBuilder.exclude(Criteria.whenHasMatch(tooManyObjects));
			});
		}
	}
}
