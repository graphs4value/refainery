/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.metamodel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.term.cardinalityinterval.CardinalityInterval;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.ConcretizationSettings;
import tools.refinery.store.reasoning.translator.multiplicity.ConstrainedMultiplicity;
import tools.refinery.store.reasoning.translator.multiplicity.Multiplicity;
import tools.refinery.store.reasoning.translator.multiplicity.UnconstrainedMultiplicity;

import java.util.*;

public final class ReferenceInfoBuilder {
	private boolean containment;
	private PartialRelation sourceType;
	private Multiplicity multiplicity = UnconstrainedMultiplicity.INSTANCE;
	private PartialRelation targetType;
	private PartialRelation opposite;
	private TruthValue defaultValue = TruthValue.UNKNOWN;
	private ConcretizationSettings concretizationSettings = new ConcretizationSettings(true, true);
	private final Set<PartialRelation> supersets = new LinkedHashSet<>();

	ReferenceInfoBuilder() {
	}

	public ReferenceInfoBuilder source(@NotNull PartialRelation sourceType) {
		if (sourceType.arity() != 1) {
			throw new IllegalArgumentException("Source type %s must be of arity 1".formatted(sourceType));
		}
		this.sourceType = sourceType;
		return this;
	}

	public ReferenceInfoBuilder target(@NotNull PartialRelation targetType) {
		if (targetType.arity() != 1) {
			throw new IllegalArgumentException("Target type %s must be of arity 1".formatted(targetType));
		}
		this.targetType = targetType;
		return this;
	}

	public ReferenceInfoBuilder containment(boolean containment) {
		this.containment = containment;
		return this;
	}

	public ReferenceInfoBuilder multiplicity(@NotNull Multiplicity multiplicity) {
		this.multiplicity = multiplicity;
		return this;
	}

	public ReferenceInfoBuilder multiplicity(@NotNull CardinalityInterval multiplicityInterval,
											 @NotNull PartialRelation errorSymbol) {
		return multiplicity(new ConstrainedMultiplicity(multiplicityInterval, errorSymbol));
	}

	public ReferenceInfoBuilder opposite(@Nullable PartialRelation opposite) {
		if (opposite != null && opposite.arity() != 2) {
			throw new IllegalArgumentException("Opposite %s must be of arity 2".formatted(targetType));
		}
		this.opposite = opposite;
		return this;
	}

	public ReferenceInfoBuilder defaultValue(@NotNull TruthValue defaultValue) {
		if (defaultValue.must()) {
			throw new IllegalArgumentException("Unsupported default value");
		}
		this.defaultValue = defaultValue;
		return this;
	}

	public ReferenceInfoBuilder concretizationSettings(ConcretizationSettings concretizationSettings) {
		this.concretizationSettings = concretizationSettings;
		return this;
	}

	public ReferenceInfoBuilder supersets(PartialRelation... supersets) {
		return this.supersets(List.of(supersets));
	}

	public ReferenceInfoBuilder supersets(Collection<PartialRelation> supersets) {
		this.supersets.addAll(supersets);
		return this;
	}

	public ReferenceInfo build() {
		if (sourceType == null) {
			throw new IllegalStateException("Source type is required");
		}
		if (targetType == null) {
			throw new IllegalStateException("Target type is required");
		}
		return new ReferenceInfo(containment, sourceType, multiplicity, targetType, opposite, defaultValue,
				concretizationSettings, Collections.unmodifiableSet(supersets));
	}
}
