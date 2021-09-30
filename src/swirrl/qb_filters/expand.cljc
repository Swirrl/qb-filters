(ns swirrl.qb-filters.expand
  (:require
   [clojure.set :as set]
   [grafter-2.rdf4j.repository :as repo]
   [selmer.parser :refer [<<]]))

(defn- concepts-where [conn where]
  (->> (str
        "PREFIX qb: <http://purl.org/linked-data/cube#>
         PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
         SELECT DISTINCT ?concept
         WHERE {" where "}")
       (repo/query conn)
       (map :concept)
       set))

(defn- expand-individual [cube dim conn concept]
  #{concept})

(defn- expand-all
  ([cube dim conn]
   (concepts-where conn
     (<< "?obs qb:dataSet <{{cube}}> ; <{{dim}}> ?concept .")))
  ([cube dim conn level]
   (concepts-where conn
     (<< "?obs qb:dataSet <{{cube}}> ; <{{dim}}> ?concept .
          ?concept a <{{level}}> ."))))

(defn- expand-descendants
  ([cube dim conn concept]
   (concepts-where conn
     (<< "?obs qb:dataSet <{{cube}}> ; <{{dim}}> ?concept .
          <{{concept}}> (skos:narrower|^skos:broader)+ ?concept .")))
  ([cube dim conn concept level]
   (concepts-where conn
     (<< "?obs qb:dataSet <{{cube}}> ; <{{dim}}> ?concept .
          <{{concept}}> (skos:narrower|^skos:broader)+ ?concept .
          ?concept a <{{level}}> ."))))

(defn- expand-action [cube dim conn acc [action [verb & args]]]
  ((case action
     :select set/union
     :deselect set/difference)
   acc
   (apply (case verb
            :individual expand-individual
            :all expand-all
            :descendants expand-descendants)
          cube
          dim
          conn
          args)))

(defn expand-dim
  "Like expand, but only expand the given dimension when that's all we want"
  [cube repo dim dim->actions]
  (with-open [conn (repo/->connection repo)]
    (reduce #(expand-action cube dim conn %1 %2) #{} (dim->actions dim))))

(defn expand
  "Takes a map of dimensions to actions representing cube filters (as returned
   by parse) and returns a map from dimensions to concrete sets of URIs."
  [cube repo dim->actions]
  (with-open [conn (repo/->connection repo)]
    (->> dim->actions
         (map (fn [[dim actions]]
                [dim
                 (reduce #(expand-action cube dim conn %1 %2) #{} actions)]))
         (into {}))))

