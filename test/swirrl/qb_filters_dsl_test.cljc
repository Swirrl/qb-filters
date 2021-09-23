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
         (dsl/parse "dimension|dEngland LSOA|!dLondon|iWales|sfoo")))
  (is (= {(->uri "dimension")
          [[:select [:all]]
           [:deselect [:search "a b|c d"]]]}
         (dsl/parse "dimension|a|!sa%20b%7Cc%20d"))))

(deftest test-serialize
  (is (= "dimension|dEngland LSOA|!dLondon|iWales|sfoo"
         (dsl/serialize
          {(->uri "dimension")
           [[:select [:descendants (->uri "England") (->uri "LSOA")]]
            [:deselect [:descendants (->uri "London")]]
            [:select [:individual (->uri "Wales")]]
            [:select [:search "foo"]]]})))
  (is (= "dimension|a|!sa%20b%7Cc%20d"
         (dsl/serialize
          {(->uri "dimension")
           [[:select [:all]]
            [:deselect [:search "a b|c d"]]]}))))
