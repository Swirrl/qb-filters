# qb-filters

Functions for working with muttnik's `qb-filters` query parameter.

`parse` a string to get an easier to work with representation of the filters,
or `serialize` to go the other way.

## Installation

Add the following to your `deps.edn`

```clojure
{:deps
 {io.github.swirrl/qb-filters
  {:git/url "git@github.com:Swirrl/qb-filters"
   :git/sha "dd986ad270f6e338d03c299f4870d5448d6d055e"}
  ,,,}
 ,,,}
```

## Representation

The result of `dsl/parse` (or the input to `dsl/serialize` or `expand/expand`)
is a map from dimension to a vector of **actions**:

```clojure
{(URI. "dim0") [action0 action1 action3]
 (URI. "dim1") [action4 action5]}
```

An **action** is either a `:select`

```clojure
[:select selection]
```

or a `:deselect`

```clojure
[:deselect selection]
```

and a **selection** is one of

```clojure
[:individual concept]
[:all]
[:all level]
[:descendants concept]
[:descendants concept level]
[:search query]
[:search query level]
```

**Note** at the time of writing only `:individual` and `:all` are supported.
More will follow as they are needed.

## Example

Generating a query parameter with `dsl/serialize`:

```clojure
(ns example
  (:require
   [swirrl.qb-filters.expand :as expand])
   [swirrl.qb-filters.dsl :as dsl]
  (:import java.net.URI))

(dsl/serialize
 {(URI. "geo")
  [[:select [:descendants (URI. "England") (URI. "LSOA")]]
   [:deselect [:descendants (URI. "London")]]
   [:select [:individual (URI. "Wales")]]]})
```

returns `geo|dEngland LSOA|!dLondon|iWales` which should finally be URI
encoded and included in the query string:

```
&qb-filters=geo%7CdEngland%20LSOA%7C!dLondon%7CiWales
```

Going the other way with `dsl/parse`, and expanding with `expand/expand`:

```clojure
(->> "http://muttnik.gov/def/dimension/multi_parent|a|!ihttp://muttnik.gov/def/concept/multi-parent/boots||http://purl.org/linked-data/cube#measureType|ihttp://muttnik.gov/def/measure/count"
     dsl/parse
     (expand/expand
      (URI. "http://muttnik.gov/data/observations-multi-parent-hierarchy")
      repo))
```

Where `repo` is a grafter RDF4J repository. Gives a map from dimension to set
of concepts:

```clojure
{(URI. "http://purl.org/linked-data/cube#measureType")
 #{(URI. "http://muttnik.gov/def/measure/count")}
 (URI. "http://muttnik.gov/def/dimension/multi_parent")
 #{(URI. "http://muttnik.gov/def/concept/multi-parent/hats")
   (URI. "http://muttnik.gov/def/concept/multi-parent/racquets")
   (URI. "http://muttnik.gov/def/concept/multi-parent/recurve-bows")
   (URI. "http://muttnik.gov/def/concept/multi-parent/running-shoes")
   (URI. "http://muttnik.gov/def/concept/multi-parent/shirts")
   (URI. "http://muttnik.gov/def/concept/multi-parent/shoes")
   (URI. "http://muttnik.gov/def/concept/multi-parent/trousers")}}
```

Or only expand the filter you care about with `expand/expand-dim`:

```clojure
(->> "http://muttnik.gov/def/dimension/multi_parent|a|!ihttp://muttnik.gov/def/concept/multi-parent/boots||http://purl.org/linked-data/cube#measureType|ihttp://muttnik.gov/def/measure/count"
     dsl/parse
     (expand/expand-dim
      (URI. "http://muttnik.gov/data/observations-multi-parent-hierarchy")
      repo
      (URI. "http://muttnik.gov/def/dimension/multi_parent")))
```

Returns the set of concepts for the given dimension

```clojure
#{(URI. "http://muttnik.gov/def/concept/multi-parent/hats")
  (URI. "http://muttnik.gov/def/concept/multi-parent/racquets")
  (URI. "http://muttnik.gov/def/concept/multi-parent/recurve-bows")
  (URI. "http://muttnik.gov/def/concept/multi-parent/running-shoes")
  (URI. "http://muttnik.gov/def/concept/multi-parent/shirts")
  (URI. "http://muttnik.gov/def/concept/multi-parent/shoes")
  (URI. "http://muttnik.gov/def/concept/multi-parent/trousers")}
```

## Grammar

Pending a more complete description (since things are still changing), here's a
sketch of the grammar of the DSL:

```
filters = dim-actions ("||" dim-actions)*
dim-actions = dim "|" action ("|" actions)*
dim = #"[^ |]+"
action = selection | deselection
selection = verb (arg (" " arg)*)?
deselection = "!" selection
verb = #"[a-z]"
arg = #"[^ |]+"
```

Or in English:

- The full expression is a `||`-separated list of dim-actions pairs
- A dim-actions pair is a dimension, `|`, and then a `|`-separated list of
  actions
- An action is a selection or a deselection
- A selection is a single letter verb followed by a space-separated list of
  (verb dependent) arguments
- A deselection is a selection prefixed with a `!`

Verbs are currently `i` for individual, `a` for all, `d` for descendants, `s`
for search.
