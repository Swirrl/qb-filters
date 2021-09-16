# qb-filters-dsl

Functions for working with muttnik's `qb-filters` query parameter.

`parse` a string to get an easier to work with representation of the filters,
or `serialize` to go the other way.

## Installation

Add the following to your `deps.edn`

```clojure
{:deps
 {swirrl/qb-filters-dsl
  {:git/url "git@github.com:Swirrl/qb-filters-dsl"
   :sha "1a64cb5e28e6a067806550dbb7da08db2fc0c613"}
  ,,,}
 ,,,}
```

## Representation

The result of `parse` (or the input to `serialize`) is a map from dimension to
a vector of **actions**:

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

## Example

```clojure
(ns example
  (:require [qb-filters-dsl.core :as dsl])
  (:import java.net.URI))

(dsl/serialize
 {(URI. "geo")
  [[:select [:descendants (URI. "England") (URI. "LSOA")]]
   [:deselect [:descendants (URI. "London")]]
   [:select [:individual (URI. "Wales")]]]})
```

returns `geo\ndEngland LSOA\n!dLondon\niWales` which should finally be URI
encoded and included in the query string:

```
&qb-filters=geo%0AdEngland%20LSOA%0A!dLondon%0AiWales
```
