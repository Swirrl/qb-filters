(ns swirrl.qb-filters-dsl
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   #?(:cljs grafter.vocabularies.core))
  #?(:clj (:import java.net.URI)))

(defn- decode-query [query]
  (-> query (str/replace "%20" " ") (str/replace "%0A" "\n")))

(defn- ->uri [s]
  #?(:clj (URI. s)
     :cljs (grafter.vocabularies.core/->uri s)))

(defn- parse-all
  ([] [:all])
  ([level] [:all (->uri level)]))

(defn- parse-descendants
  ([concept] [:descendants (->uri concept)])
  ([concept level]
   [:descendants (->uri concept) (->uri level)]))

(defn- parse-individual [concept]
  [:individual (->uri concept)])

(defn- parse-search
  ([query] [:search (decode-query query)])
  ([query level] [:search (decode-query query) (->uri level)]))

(defn- parse-selection [s]
  (apply
   (case (get s 0)
     \a parse-all
     \d parse-descendants
     \i parse-individual
     \s parse-search)
   (filter seq (str/split (subs s 1) #" "))))

(defn- parse-action [s]
  (if (= \! (get s 0))
    [:deselect (parse-selection (subs s 1))]
    [:select (parse-selection s)]))

(defn parse
  "Takes a string representing qb-filters, and parses it to a map of dimension
   to actions for further manipulation or feeding to expand."
  [s]
  (->> (str/split s #"\n\n")
       (filter seq)
       (map (fn [paragraph]
              (let [[dim actions] (str/split paragraph #"\n" 2)]
                [(->uri dim)
                 (mapv parse-action (str/split-lines actions))])))
       (into {})))

(defn- encode-query [query]
  (-> query (str/replace " " "%20") (str/replace "\n" "%0A")))

(defn- serialize-all
  ([] "a")
  ([level] (str "a" level)))

(defn- serialize-descendants
  ([concept] (str "d" concept))
  ([concept level] (str "d" concept " " level)))

(defn- serialize-individual [concept]
  (str "i" concept))

(defn- serialize-search
  ([query] (str "s" (encode-query query)))
  ([query level] (str "s" (encode-query query) " " level)))

(defn- serialize-selection [[verb & args]]
  (case verb
    :individual (apply serialize-individual args)
    :all (apply serialize-all args)
    :descendants (apply serialize-descendants args)
    :search (apply serialize-search args)))

(defn- serialize-action [[action selection]]
  (case action
    :select (serialize-selection selection)
    :deselect (str "!" (serialize-selection selection))))

(defn serialize
  "Inverse of parse"
  [dims-actions]
  (->> dims-actions
       (map (fn [[dim actions]]
              (when (and dim (not-empty actions))
                (str dim
                     "\n"
                     (->> actions (map serialize-action) (str/join "\n"))))))
       (str/join "\n\n")))
