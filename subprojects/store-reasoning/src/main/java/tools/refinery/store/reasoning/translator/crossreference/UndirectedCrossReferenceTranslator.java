/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.crossreference;

import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.view.ForbiddenView;
import tools.refinery.store.reasoning.lifting.DnfLifter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.RoundingMode;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.reasoning.translator.multiplicity.InvalidMultiplicityErrorTranslator;
import tools.refinery.store.representation.Symbol;

import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.add;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.remove;
import static tools.refinery.store.reasoning.literal.PartialLiterals.*;
import static tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator.MULTI_VIEW;

public class UndirectedCrossReferenceTranslator implements ModelStoreConfiguration {
	private final PartialRelation linkType;
	private final UndirectedCrossReferenceInfo info;
	private final Symbol<TruthValue> symbol;

	public UndirectedCrossReferenceTranslator(PartialRelation linkType, UndirectedCrossReferenceInfo info) {
		this.linkType = linkType;
		this.info = info;
		symbol = Symbol.of(linkType.name(), 2, TruthValue.class, info.defaultValue());
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		var type = info.type();
		var defaultValue = info.defaultValue();
		if (defaultValue.must()) {
			throw new TranslationException(linkType, "Unsupported default value %s for undirected cross reference %s"
					.formatted(defaultValue, linkType));
		}
		var translator = PartialRelationTranslator.of(linkType);
		translator.symbol(symbol);
		if (defaultValue.may()) {
			configureWithDefaultUnknown(translator);
		} else {
			configureWithDefaultFalse(storeBuilder);
		}
		translator.initializer(new UndirectedCrossReferenceInitializer(linkType, symbol));
		translator.refiner(UndirectedCrossReferenceRefiner.of(symbol, type));
		if (info.partial()) {
			translator.roundingMode(RoundingMode.NONE);
		} else {
			translator.decision(Rule.of(linkType.name(), (builder, source, target) -> builder
					.clause(
							may(linkType.call(source, target)),
							not(candidateMust(linkType.call(source, target))),
							not(MULTI_VIEW.call(source)),
							not(MULTI_VIEW.call(target))
					)
					.action(
							add(linkType, source, target)
					)));
		}
		storeBuilder.with(translator);
		storeBuilder.with(new InvalidMultiplicityErrorTranslator(type, linkType, false, info.multiplicity()));
	}

	private void configureWithDefaultUnknown(PartialRelationTranslator translator) {
		var name = linkType.name();
		var type = info.type();
		var multiplicity = info.multiplicity();
		var mayName = DnfLifter.decorateName(name, Modality.MAY, Concreteness.PARTIAL);
		var mayNewSource = CrossReferenceUtils.createMayHelper(linkType, type, multiplicity, false);
		var forbiddenView = new ForbiddenView(symbol);
		translator.may(Query.of(mayName, (builder, source, target) -> {
			builder.clause(
					mayNewSource.call(source),
					mayNewSource.call(target),
					not(forbiddenView.call(source, target))
			);
			if (info.isConstrained()) {
				// Violation of monotonicity:
				// Edges violating upper multiplicity will not be marked as {@code ERROR}, but the
				// corresponding error pattern will already mark the node as invalid.
				builder.clause(
						must(linkType.call(source, target)),
						not(forbiddenView.call(source, target)),
						may(type.call(source)),
						may(type.call(target))
				);
			}
		}));
		if (info.partial()) {
			var candidateMayName = DnfLifter.decorateName(name, Modality.MAY, Concreteness.CANDIDATE);
			var candidateMayNewSource = CrossReferenceUtils.createCandidateMayHelper(linkType, type, multiplicity,
					false);
			translator.candidateMay(Query.of(candidateMayName, (builder, source, target) -> {
				builder.clause(
						candidateMayNewSource.call(source),
						candidateMayNewSource.call(target),
						not(forbiddenView.call(source, target))
				);
				if (info.isConstrained()) {
					builder.clause(
							candidateMust(linkType.call(source, target)),
							not(forbiddenView.call(source, target)),
							candidateMay(type.call(source)),
							candidateMay(type.call(target))
					);
				}
			}));
		}
	}

	private void configureWithDefaultFalse(ModelStoreBuilder storeBuilder) {
		var name = linkType.name();
		var type = info.type();
		var mayNewSource = CrossReferenceUtils.createMayHelper(linkType, type, info.multiplicity(), false);
		// Fail if there is no {@link PropagationBuilder}, since it is required for soundness.
		var propagationBuilder = storeBuilder.getAdapter(PropagationBuilder.class);
		propagationBuilder.rule(Rule.of(name + "#invalidLink", (builder, p1, p2) -> {
			builder.clause(
					may(linkType.call(p1, p2)),
					not(may(type.call(p1)))
			);
			if (info.isConstrained()) {
				builder.clause(
						may(linkType.call(p1, p2)),
						not(must(linkType.call(p1, p2))),
						not(mayNewSource.call(p1))
				);
			}
			builder.action(
					remove(linkType, p1, p2)
			);
		}));
	}
}
