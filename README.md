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
  (:require [swirrl.qb-filters.dsl :as dsl])
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
