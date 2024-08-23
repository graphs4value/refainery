/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics;

import com.google.inject.Inject;
import org.eclipse.collections.api.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import tools.refinery.language.model.problem.*;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.reasoning.translator.metamodel.Metamodel;

import java.util.*;

class ProblemTraceImpl implements ProblemTrace {
	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Inject
	private SemanticsUtils semanticsUtils;

	@Inject
	private IScopeProvider scopeProvider;

	private Problem problem;
	private Metamodel metamodel;
	private final MutableObjectIntMap<Node> mutableNodeTrace = ObjectIntMaps.mutable.empty();
	private final ObjectIntMap<Node> nodeTrace = mutableNodeTrace.asUnmodifiable();
	private final Map<Relation, PartialRelation> mutableRelationTrace = new LinkedHashMap<>();
	private final Map<Relation, PartialRelation> relationTrace =
			Collections.unmodifiableMap(mutableRelationTrace);
	private final Map<AnyPartialSymbol, Relation> mutableInverseTrace = new HashMap<>();
	private final Map<AnyPartialSymbol, Relation> inverseTrace = Collections.unmodifiableMap(mutableInverseTrace);
	private final Map<Rule, RuleDefinition> mutableInverseRuleDefinitionTrace = new LinkedHashMap<>();
	private final Map<Rule, RuleDefinition> inverseRuleDefinitionTrace = Collections.unmodifiableMap(
			mutableInverseRuleDefinitionTrace);

	@Override
	public Problem getProblem() {
		return problem;
	}

	void setProblem(Problem problem) {
		this.problem = problem;
	}

	@Override
	public Metamodel getMetamodel() {
		return metamodel;
	}

	void setMetamodel(Metamodel metamodel) {
		this.metamodel = metamodel;
	}

	@Override
	public ObjectIntMap<Node> getNodeTrace() {
		return nodeTrace;
	}

	int collectNode(Node node) {
		var nextId = mutableNodeTrace.size();
		mutableNodeTrace.getIfAbsentPut(node, nextId);
		return nextId;
	}

	@Override
	public int getNodeId(Node node) {
		try {
			return nodeTrace.getOrThrow(node);
		} catch (IllegalStateException e) {
			var qualifiedName = semanticsUtils.getNameWithoutRootPrefix(node);
			throw new TracedException(node, "No node ID for " + qualifiedName, e);
		}
	}

	@Override
	public int getNodeId(QualifiedName qualifiedName) {
		var nodeScope = scopeProvider.getScope(problem, ProblemPackage.Literals.NODE_ASSERTION_ARGUMENT__NODE);
		return getNodeId(getElement(nodeScope, qualifiedName, Node.class));
	}

	@Override
	public int getNodeId(String qualifiedName) {
		var convertedName = qualifiedNameConverter.toQualifiedName(qualifiedName);
		return getNodeId(convertedName);
	}

	@Override
	public Map<Relation, PartialRelation> getRelationTrace() {
		return relationTrace;
	}

	void putRelation(Relation relation, PartialRelation partialRelation) {
		var oldPartialRelation = mutableRelationTrace.put(relation, partialRelation);
		if (oldPartialRelation != null) {
			throw new TracedException(relation, "Relation already mapped to partial relation: " + oldPartialRelation);
		}
		var oldRelation = mutableInverseTrace.put(partialRelation, relation);
		if (oldRelation != null) {
			throw new TracedException(oldRelation, "Partial relation %s was already mapped to relation"
					.formatted(partialRelation));
		}
	}

	void removeShadowRelations() {
		var iterator = mutableRelationTrace.entrySet().iterator();
		while (iterator.hasNext()) {
			var entry = iterator.next();
			var relation = entry.getKey();
			if (relation instanceof PredicateDefinition predicateDefinition &&
					predicateDefinition.getKind() == PredicateKind.SHADOW) {
				iterator.remove();
				mutableInverseTrace.remove(entry.getValue());
			}
		}
	}

	@Override
	public Map<AnyPartialSymbol, Relation> getInverseRelationTrace() {
		return inverseTrace;
	}

	@Override
	public Relation getRelation(AnyPartialSymbol partialSymbol) {
		var relation = mutableInverseTrace.get(partialSymbol);
		if (relation == null) {
			throw new IllegalArgumentException("No relation for partial symbol: " + partialSymbol);
		}
		return relation;
	}

	void putRuleDefinition(RuleDefinition ruleDefinition, Rule rule) {
		var oldRuleDefinition = mutableInverseRuleDefinitionTrace.put(rule, ruleDefinition);
		if (oldRuleDefinition != null) {
			throw new TracedException(oldRuleDefinition, "Rule %s was already mapped to rule definition"
					.formatted(rule.getName()));
		}
	}

	void putPropagationRuleDefinition(RuleDefinition ruleDefinition, Collection<Rule> rules) {
		for (var rule : rules) {
			putRuleDefinition(ruleDefinition, rule);
		}
	}

	@Override
	public Map<Rule, RuleDefinition> getInverseRuleDefinitionTrace() {
		return inverseRuleDefinitionTrace;
	}

	@Override
	public RuleDefinition getRuleDefinition(Rule rule) {
		var ruleDefinition = mutableInverseRuleDefinitionTrace.get(rule);
		if (ruleDefinition == null) {
			throw new IllegalArgumentException("No definition for rule: " + rule.getName());
		}
		return ruleDefinition;
	}

	@Override
	public RuntimeException wrapException(TranslationException translationException) {
		var partialSymbol = translationException.getPartialSymbol();
		if (partialSymbol == null) {
			return translationException;
		}
		var relation = mutableInverseTrace.get(partialSymbol);
		if (relation == null) {
			return translationException;
		}
		return new TracedException(relation, translationException);
	}

	@Override
	public PartialRelation getPartialRelation(Relation relation) {
		var partialRelation = mutableRelationTrace.get(relation);
		if (partialRelation == null) {
			var qualifiedName = semanticsUtils.getNameWithoutRootPrefix(relation);
			throw new TracedException(relation, "No partial relation for " + qualifiedName);
		}
		return partialRelation;
	}

	@Override
	public PartialRelation getPartialRelation(QualifiedName qualifiedName) {
		var relationScope = scopeProvider.getScope(problem, ProblemPackage.Literals.ABSTRACT_ASSERTION__RELATION);
		return getPartialRelation(getElement(relationScope, qualifiedName, Relation.class));
	}

	@Override
	public PartialRelation getPartialRelation(String qualifiedName) {
		var convertedName = qualifiedNameConverter.toQualifiedName(qualifiedName);
		return getPartialRelation(convertedName);
	}

	private <T> T getElement(IScope scope, QualifiedName qualifiedName, Class<T> type) {
		return semanticsUtils.getElement(problem, scope, qualifiedName, type);
	}
}
