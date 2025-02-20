---
SPDX-FileCopyrightText: 2024 The Refinery Authors
SPDX-License-Identifier: EPL-2.0
description: Model queries and model validation
sidebar_position: 2
---

# Graph predicates

Graph predicates are logic expressions that can be used to query for interesting model fragments, as well as for validating the consistency of models. They are evaluated on partial models according to [four-valued logic](../logic) semantics.

Predicates in Refinery are written in [Disjunctive Normal Form](https://en.wikipedia.org/wiki/Disjunctive_normal_form) (DNF) as an _OR_ of _ANDs_, i.e., a _disjunction_ of _clauses_ formed as a _conjunction_ of positive or negated logic _literals._
This matches the syntax and semantics of logical query languages, such as [Datalog](https://en.wikipedia.org/wiki/Datalog), and logical programming languages, such as [Prolog](https://en.wikipedia.org/wiki/Prolog).

import Link from '@docusaurus/Link';

<details>
<summary>Example metamodel</summary>

In the examples on this page, we will use the following metamodel as illustration:

```refinery
abstract class CompositeElement {
    contains Region[] regions
}

class Region {
    contains Vertex[] vertices opposite region
}

abstract class Vertex {
    container Region region opposite vertices
    contains Transition[] outgoingTransition opposite source
    Transition[] incomingTransition opposite target
}

class Transition {
    container Vertex source opposite outgoingTransition
    Vertex[1] target opposite incomingTransition
}

abstract class Pseudostate extends Vertex.

abstract class RegularState extends Vertex.

class Entry extends Pseudostate.

class Exit extends Pseudostate.

class Choice extends Pseudostate.

class FinalState extends RegularState.

class State extends RegularState, CompositeElement.

class Statechart extends CompositeElement.
```

<p>
  <Link
    href="https://refinery.services/#/1/KLUv_WAEAiUIAOIKIR5gadMGg1ajk9jLoipJ58vc0vAE5opt1YaDpyOCAAdaCjMohSdgl4rj1yTo8UCgpTDHCIAE-o3Jr28mGO9AEoDcR-tLGh4liE2Z3IOX50z-FksLaNWLpLXd1QiUII2vNjCMBWOVEgTzjhG0eHVMIyIyFOjoxcrBv83FkgftlmJ0K_0eVDQgEBSCrXYvD1Q2wlwGXecz2HjRADQOLMh6iIYIWBPuFBBCI2igVgiHAFH4uclAydd4TFayN-oOpjzxgd0FlTzkN6QZ8CQDXBN4EPjB5VJZCANQlJA3wDd_PVyUA5eA0gaeAcgENsm4YnCogWihMAMkA8-CoB-gm9HJC0AB"
    className="button button--lg button--primary button--play"
  >Try in Refinery</Link>
</p>

</details>

[Assertions](../logic/#assertions) about graph predicates can prescribe where the predicate should (for positive assertions) or should not (for negative assertions) hold.
When generating consistent models

## Atoms

An _atom_ is formed by a _symbol_ and _argument list_ of variables.
Possible symbols include [classes](../classes/#classes), [references](../classes/#references), and [predicates](../predicates).
We may write a basic graph query as a conjunction (AND) of atoms.

The `pred` keyword defines a graph predicate. After the _predicate name_, a _parameter list_ of variables is provided. The atoms of the graph predicate are written after the `<->` operator, and a full stop `.` terminates the predicate definition.

The following predicate `entryInRegion` will match pairs of `Region` instances `r` and `Entry` instances `e` such that `e` is a vertex in `r`.

```refinery
pred entryInRegion(r, e) <->
    Region(r),
    vertices(r, e),
    Entry(e).
```

We may write unary symbols that act as _parameter types_ directly in the parameter list. The following definition is equivalent to the previous one:

```refinery
pred entryInRegion(Region r, Entry e) <->
    vertices(r, e).
```

import TableIcon from '@material-icons/svg/svg/table_chart/baseline.svg';

:::info

You may display the result of graph predicate matching in the <TableIcon style={{ fill: 'currentColor', verticalAlign: 'text-top' }} title="Table view icon" />&nbsp;_table view_ of the Refinery web UI.

:::

## Quantification

Variables not appearing in the parameter list are _existentially quantified._

The following predicate matches `Region` instances with two entries:

```refinery
pred multipleEntriesInRegion(Region r) <->
    entryInRegion(r, e1),
    entryInRegion(r, e2),
    e1 != e2.
```

Existentially quantified variables that appear only once in the predicate should be prefixed with `_`. This shows that the variable is intentionally used only once (as opposite to the second reference to the variable being omitted by mistake).

```refinery
pred regionWithEntry(Region r) <->
    entryInRegion(r, _e).
```

Alternatively, you may use a single `_` whenever a variable occurring only once is desired. Different occurrences of `_` are considered distinct variables.

```refinery
pred regionWithEntry(Region r) <->
    entryInRegion(r, _).
```

## Negation

Negative literals are written by prefixing the corresponding atom with `!`.

Inside negative literals, quantification is _universal:_ the literal matches if there is no assignment of the variables solely appearing in it that satisfies the corresponding atom.

The following predicate matches `Region` instances that have no `Entry`:

```refinery
pred regionWithoutEntry(Region r) <->
    !entryInRegion(r, _).
```

In a graph predicate, all parameter variables must be _positively bound,_ i.e., appear in at least one positive literal (atom).
Negative literals may further constrain the predicate match once it has been established by the positive literals.

## Object equality

The operators `a == b` and `a != b` correspond to the literals `equals(a, b)` and `!equals(a, b)`, respectively.
See the section about [multi-objects](../logic/#multi-objects) for more information about the `equals` symbol.

## Transitive closure

The `+` operator forms the [transitive closure](https://en.wikipedia.org/wiki/Transitive_closure) of symbols with exactly 2 parameters.
The transitive closure `r+(a, b)` holds if either `r(a, b)` is `true`, or there is a sequence of objects `c1`, `c2`, &hellip;, `cn` such that `r(a, c1)`, `r(c1, c2)`, `r(c2, c3)`, &hellip;, `r(cn, b)`.
In other words, there is a path labelled with `r` in the graph from `a` to `b`.

Transitive closure may express queries about graph reachability:

```refinery
pred neighbors(Vertex v1, Vertex v2) <->
    Transition(t),
    source(t, v1),
    target(t, v2).

pred cycle(Vertex v) <->
    neighbors+(v, v).
```

## Disjunction

Disjunction (OR) of _clauses_ formed by a conjunction (AND) of literals is denoted by `;`.

```refinery
pred regionWithInvalidNumberOfEntries(Region r) <->
    !entryInRegion(r, _)
;
    entryInRegion(r, e1),
    entryInRegion(r, e2),
    e1 != e2.
```

Every clause of a disjunction must bind every parameter variable of the graph predicate _positively._
_Type annotations_ on parameter are applied in all clauses.
Therefore, the previous graph pattern is equivalent to the following:

```refinery
pred regionWithInvalidNumberOfEntries(r) <->
    Region(r),
    !entryInRegion(r, _)
;
    Region(r),
    entryInRegion(r, e1),
    entryInRegion(r, e2),
    e1 != e2.
```

## Derived features

Graph predicates may act as _derived types_ and _references_ in metamodel.

A graph predicate with exactly 1 parameter can be used as if it was a class: you may use it as a [_parameter type_](#atoms) in other graph patterns, as a _target type_ of a (non-containment) [reference](../classes/#references), or in a [_scope constraint_](../logic#type-scopes).

_Derived references_ are graph predicates with exactly 2 parameters, which correspond the source and target node of the reference.

import TuneIcon from '@material-icons/svg/svg/tune/baseline.svg';
import LabelIcon from '@material-icons/svg/svg/label/baseline.svg';
import LabelOutlineIcon from '@material-icons/svg/svg/label/outline.svg';

:::info

You may use the <TuneIcon style={{ fill: 'currentColor', verticalAlign: 'text-top' }} title="Filter panel icon" />&nbsp;_filter panel_ icon in Refinery to toggle the visibility of graph predicates with 1 or 2 parameters.
You may either show <LabelOutlineIcon style={{ fill: 'currentColor', verticalAlign: 'text-top' }} title="Unknown value icon" />&nbsp;_both true and unknown_ values or <LabelIcon style={{ fill: 'currentColor', verticalAlign: 'text-top' }} title="True value icon" />&nbsp;_just true_ values.

:::

---

For example, we may replace the reference `neighbors` in the class `Vertex`:

```refinery
class Vertex {
    Vertex[] neighbors
}
```

with the graph predicate `neighbors` as follows:


```refinery
class Vertex {
    contains Transition[] outgoingTransition opposite source
    Transition[] incomingTransition opposite target
}

class Transition {
    container Vertex source opposite outgoingTransition
    Vertex[1] target opposite incomingTransition
}

pred neighbors(Vertex v1, Vertex v2) <->
    Transition(t),
    source(t, v1),
    target(t, v2).
```

Since `neighbors` is now computed based on the `Transition` instances and their `source` and `target` references present in the model, the assertion

```refinery
neighbors(vertex1, vertex2).
```

will only be satisfied if a corresponding node `transition1` is present in the generated model that also satisfies

```refinery
Transition(transition1).
source(transition1, vertex1).
target(transition1, vertex2).
```

import DerivedFeature from './DerivedFeature.svg';

<DerivedFeature />

## Error predicates

A common use-case for graph predicates is _model validation_, where a predicate highlights _errors_ in the model.
Such predicates are called _error predicates._
In a consistent generated model, an error predicates should have no matches.

You can declare error predicates with the `error` keyword:

```refinery
error regionWithoutEntry(Region r) <->
    !entryInRegion(r, _).
```

This is equivalent to asserting that the error predicate is `false` everywhere:

```refinery
pred regionWithoutEntry(Region r) <->
    !entryInRegion(r, _).

!regionWithoutEntry(*).
```
