---
SPDX-FileCopyrightText: 2024 The Refinery Authors
SPDX-License-Identifier: EPL-2.0
description: Four-valued logic abstraction
sidebar_position: 1
---

# Partial modeling

Refinery allows precisely expressing _unknown,_ _uncertain_ or even _contradictory_ information using [four-valued logic](https://en.wikipedia.org/wiki/Four-valued_logic#Belnap).
During model generation, unknown aspects of the partial model get _refined_ into concrete (true or false) facts until the generated model is completed, or a contradiction is reached.

The _Belnap--Dunn four-valued logic_ supports the following truth values:

* `true` values correspond to facts known about the model, e.g., that a node is the instance of a given class or there is a reference between two nodes.
* `false` values correspond to facts that are known not to hold, e.g., that a node is _not_ an instance of a given class or there is _no_ reference between two nodes.
* `unknown` values express uncertain properties and design decisions yet to be made. During model refinement, `unknown` values are gradually replaced with `true` and `false` values until a consistent and concrete model is derived.
* `error` values represent contradictions and validation failures in the model. Once a model contains an error value, it can't be refined into a consistent model anymore.

## Assertions

_Assertions_ express facts about a partial model. An assertion is formed by a _symbol_ and an _argument list_ of _nodes_ in parentheses.
Possible symbols include [classes](../classes/#classes), [references](../classes/#references), and [predicates](../predicates).
Nodes appearing in the argument list are automatically added to the model.

A _negative_ assertion with a `false` truth value is indicated by prefixing it with `!`.

---

Consider the following metamodel:

```refinery
class Region {
    contains Vertex[] vertices
}
class Vertex.
class State extends Vertex.
```

Along with the following set of assertions:

```refinery
Region(r1).
Vertex(v1).
Vertex(v2).
!State(v2).
vertices(r1, v1).
vertices(r1, v2).
!vertices(Region::new, v1).
!vertices(Region::new, v2).
```

import AssertionsExample from './AssertionsExample.svg';

<AssertionsExample />

It is `true` that `r1` is an instance of the class `Region`, while `v1` and `v2` are instances of the class `Vertex`.
We also assert that `v2` is _not_ an instance of the class `State`, but it is unknown whether `v1` is an instance of the class `State`.
Types that are `unknown` are shown in a lighter color and with an outlined icon.

It is `true` that there is a `vertices` reference between `r1` and `v1`, as well as `r1` and `v2`, but there is no such reference from `Region::new` to the same vertices.
As no information is provided, it is `unknown` whether `State::new` is a vertex of any `Region` instance.
References that are `unknown` are shown in a lighter color and with a dashed line.

### Propagation

Refinery can automatically infer some facts about the partial model based on the provided assertions by information _propagation._
The set of assertions in the [example above](#assertions) is equivalent to the following:

```refinery
vertices(r1, v1).
vertices(r1, v2).
!State(v2).
```

By the type constraints of the `vertices` reference, Refinery can infer that `r1` is a `Region` instance and `v1` and `v2` are `Vertex` instances.
Since `State` is a subclass of `Vertex`, it is still unknown whether `v1` is a `State` instance,
but `v2` is explicitly forbidden from being such by the negative assertion `!State(v2)`.
We may omit `!vertices(Region::new, v1)` and `!vertices(Region::new, v2)`, since `v1` and `v2` may be a target of only one [containment](../classes/#containment-hierarchy) reference.

Contradictory assertions lead to `error` values in the partial model:

```refinery
State(v1).
!Vertex(v1).
```

import AssertionsError from './AssertionsError.svg';

<AssertionsError />

### Default assertions

Assertions marked with the `default` keyword have _lower priority_ than other assertions.
They may contain wildcard arguments `*` to specify information about _all_ nodes in the graph.
However, they can be overridden by more specific assertions that are not marked with the `default` keyword.

---

To make sure that the reference `vertices` is `false` everywhere except where explicitly asserted, we may add a `default` assertion:

```refinery
default !vertices(*, *).
vertices(r1, v1).
vertices(r2, v2).
vertices(r3, v3).
?vertices(r1, State::new).
```

import DefaultAssertions from './DefaultAssertions.svg';

<DefaultAssertions />

We can prefix an assertion with `?` to explicitly assert that some fact about the partial model is `unknown`.
This is useful for overriding negative `default` assertions.

## Multi-objects

The special symbols `exists` and `equals` control the _number of graph nodes_ represented by an object in a partial model.

By default, `exists` is `true` for all objects.
An object `o` with `?exists(o)` (i.e., `exists(o)` explicitly set to `unknown`) may be _removed_ from the partial model.
Therefore, it represents _at least 0_ graph nodes.

By default, `equals` is `true` for its _diagonal_, i.e., we have `equals(o, o)` for all object `o`.
For off-diagonal pairs, i.e., `(p, q)` with `p` not equal to `q`, we always have `!equals(p, q)`: distinct objects can never be _merged._
If we set a _diagonal_ entry to `unknown` by writing `?equals(o, o)`, the object `o` becomes a **multi-object:** it can be freely _split_ into multiple graph nodes.
Therefore, multi-objects represent _possibly more than 1_ graph nodes.

| `exists(o)` | `equals(o, o)` | Number of nodes | Description |
|:-:|:-:|-:|:-|
| `true` | `true` | `1` | graph node |
| `unknown` | `true` | `0..1` | removable graph node |
| `true` | `unknown` | `1..*` | multi-object |
| `unknown` | `unknown` | `0..*` | removable multi-object |

In the Refinery web UI, `?exists(o)` is represented with a _dashed_ border, while `?equals(o, o)`

```refinery
node(node).

node(removable).
?exists(removable).

node(multi).
?equals(multi, multi).

node(removableMulti).
?exists(removableMulti).
?equals(removableMulti, removableMulti).
```

import MultiObjects from './MultiObjects.svg';

<MultiObjects />

import TuneIcon from '@material-icons/svg/svg/tune/baseline.svg';
import LabelIcon from '@material-icons/svg/svg/label/baseline.svg';
import LabelOutlineIcon from '@material-icons/svg/svg/label/outline.svg';

:::info

You may use the <TuneIcon style={{ fill: 'currentColor', verticalAlign: 'text-top' }} title="Filter panel icon" />&nbsp;_filter panel_ icon in Refinery to toggle the visibility of special symbols like `exists` and `equals`.
You may either show <LabelOutlineIcon style={{ fill: 'currentColor', verticalAlign: 'text-top' }} title="Unknown value icon" />&nbsp;_both true and unknown_ values or <LabelIcon style={{ fill: 'currentColor', verticalAlign: 'text-top' }} title="True value icon" />&nbsp;_just true_ values.
The _object scopes_ toggle will also show the number of graph nodes represented by an object in square brackets after its name, like in the figure above.
:::

By default, a **new object** `C::new` is added for each non-`abstract` [class](../classes#classes) `C` with `?exists(C::new)` and `?equals(C::new, C::new)`.
This multi-object represents all potential instances of the class.
To assert that no new instances of `C` should be added to the generated model, you may write `!exists(C::new)`.

You may use the `multi` keyword to quickly defined a (removable) multi-object:

```refinery
multi removableMulti.
% This is equivalent to:
% ?exists(removableMulti).
% ?equals(removableMulti, removableMulti).
```

## Type scopes

_Type scopes_ offer finer-grained control over the number of graph nodes in the generated model (as represented by the multi-objects) than `exists` or `equals` assertions.

A _type scope constraint_ is formed by a unary symbol (a [class](../classes/#classes) or a [predicate](../predicates) with a single parameter) and _scope range._
Ranges have a form similar to [multiplicity constraints](../classes#multiplicity): a range `n..m` indicates a lower bound of `n` and an upper bound of `m`.
While an upper bound of `*` indicates a possibly unbounded number of objects, generated models will always be finite.
Like for multiplicity constraints, the case `n..n` can be abbreviated as `n`.

The number of nodes in the generated model can be controlled using the `node` special symbol.
For example, we may write the following to generate a model with at least 100 at and most 120 nodes:

```refinery
scope node = 100..200.
```

A `scope` declaration may prescribe type scope constraint for any number of symbols, separated by `,`.
Multiple `scope` declarations are also permitted.
Multiple ranges provided for the same symbol will be intersected, i.e., they influence the generated model simultaneously.

In other words,
```
scope Region = 10, State = 80..120.
scope State = 100..150.
% Equivalent to:
scope Region = 10, State = 100..120.
```

The _object scopes_ option in the <TuneIcon style={{ fill: 'currentColor', verticalAlign: 'text-top' }} title="Filter panel icon" />&nbsp;_filter panel_ may help in exploring the effects of object scopes.

---

Consider the example

```refinery
class Region {
    contains Vertex[] vertices
}
class Vertex.
class State extends Vertex.
scope node = 100..120, Vertex = 50..*.
```

import ObjectScopes from './ObjectScopes.svg';

<ObjectScopes />

Notice that Refinery could determine that there can be no more than 70 `Region` instances in the generated model, since at least 50 of the `100..120` nodes in the model must be `Vertex` instances.
However, since `State` is a subclass of `Vertex` (i.e., `State::new` is also an instance of `Vertex`), the range `50..*` is shared between both `Vertex::new` and `State::new`, resulting in both representing `0..120` nodes.
Nevertheless, every generated model will obey the scope constraint exactly, i.e., will have between 100 and 120 node, at least 50 of which are `Vertex` instances.

By providing more information, Refinery can determine more precise ranges for multi-objects.
For example, we may strengthen the scope constraints as follows:

```refinery
scope node = 100..120, Vertex = 50..*, State = 20.
```

import StrongerObjectScopes from './StrongerObjectScopes.svg';

<StrongerObjectScopes />

### Incremental scopes

We may specify an _incremental_ object scope with the `+=` operator to determine the number of new instances to be added to the model.
This is only allowed for symbols that are classes with no subclasses, as it directly influences the number of nodes represented by the corresponding `::new` object.

For example, to ensure that between 5 and 7 `State` instances are added to the model, we may write:

```refinery
State(s1).
State(s2).
scope State += 5..7.
```

Since `s1` and `s2` are also instances of the `State` class, the generated concrete model will have between 7 and 9 `State` instances altogether.
