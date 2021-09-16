(ns swirrl.qb-filters-dsl-test
  (:require
   [clojure.test :refer [deftest is]]
   [swirrl.qb-filters-dsl :as dsl]
   #?(:cljs grafter.vocabularies.core))
  #?(:clj (:import java.net.URI)))

(defn- ->uri [s]
  #?(:clj (URI. s)
     :cljs (grafter.vocabularies.core/->uri s)))

(deftest test-parse
  (is (= {(->uri "dimension")
          [[:select [:descendants (->uri "England") (->uri "LSOA")]]
           [:deselect [:descendants (->uri "London")]]
           [:select [:individual (->uri "Wales")]]
           [:select [:search "foo"]]]}
         (dsl/parse "dimension\ndEngland LSOA\n!dLondon\niWales\nsfoo")))
  (is (= {(->uri "dimension")
          [[:select [:all]]
           [:deselect [:search "a b\nc d"]]]}
         (dsl/parse "dimension\na\n!sa%20b%0Ac%20d"))))

(deftest test-serialize
  (is (= "dimension\ndEngland LSOA\n!dLondon\niWales\nsfoo"
         (dsl/serialize
          {(->uri "dimension")
           [[:select [:descendants (->uri "England") (->uri "LSOA")]]
            [:deselect [:descendants (->uri "London")]]
            [:select [:individual (->uri "Wales")]]
            [:select [:search "foo"]]]})))
  (is (= "dimension\na\n!sa%20b%0Ac%20d"
         (dsl/serialize
          {(->uri "dimension")
           [[:select [:all]]
            [:deselect [:search "a b\nc d"]]]}))))
