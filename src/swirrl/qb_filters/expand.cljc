(ns swirrl.qb-filters.expand
  (:require
   [clojure.set :as set]
   [grafter-2.rdf4j.repository :as repo]
   [selmer.parser :refer [<<]]))

(defn- concepts-where [{:keys [conn limit]} where]
  (let [concepts (->> (str
                       "PREFIX qb: <http://purl.org/linked-data/cube#>
                        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
                        SELECT DISTINCT ?concept
                        WHERE {" where "}"
                       (when limit
                         (str "\nLIMIT " (inc limit))))
                      (repo/query conn)
                      (map :concept)
                      set)]
    (when (and limit (< limit (count concepts)))
      (throw (ex-info "Selection over limit" {:type ::selection-over-limit
                                              :limit limit
                                              :where where})))
    concepts))

(defn- expand-individual [{:keys [cube dim conn]} concept]
  #{concept})

(defn- expand-all
  ([{:keys [cube dim conn] :as opts}]
   (concepts-where opts
     (<< "?obs qb:dataSet <{{cube}}> ; <{{dim}}> ?concept .")))
  ([{:keys [cube dim conn] :as opts} level]
   (concepts-where opts
     (<< "?concept a <{{level}}> .
          FILTER EXISTS {
            ?obs qb:dataSet <{{cube}}> ; <{{dim}}> ?concept .
          }"))))

(defn- expand-descendants
  ([{:keys [cube dim conn] :as opts} concept]
   (concepts-where opts
     (<< "<{{concept}}> skos:narrower+ ?concept .
          FILTER EXISTS {
            ?obs qb:dataSet <{{cube}}> ; <{{dim}}> ?concept .
          }")))
  ([{:keys [cube dim conn] :as opts} concept level]
   (concepts-where opts
     (<< "<{{concept}}> skos:narrower+ ?concept .
          ?concept a <{{level}}> .
          FILTER EXISTS {
            ?obs qb:dataSet <{{cube}}> ; <{{dim}}> ?concept .
          }"))))

(defn- expand-action [opts acc [action [verb & args]]]
  ((case action
     :select set/union
     :deselect set/difference)
   acc
   (apply (case verb
            :individual expand-individual
            :all expand-all
            :descendants expand-descendants)
          opts
          args)))

(defn expand-dim
  "Like expand, but only expand the given dimension when that's all we want"
  [opts dim dim->actions]
  (with-open [conn (repo/->connection (:repo opts))]
    (let [opts (-> opts (dissoc :repo) (assoc :conn conn))]
      (reduce #(expand-action (assoc opts :dim dim) %1 %2)
              #{}
              (dim->actions dim)))))

(defn expand
  "Takes a map of dimensions to actions representing cube filters (as returned
   by parse) and returns a map from dimensions to concrete sets of URIs.

   Required: repo, cube; optional: limit.

   If any subselection exceeds limit, an exception is thrown."
  [opts dim->actions]
  (with-open [conn (repo/->connection (:repo opts))]
    (let [opts (-> opts (dissoc :repo) (assoc :conn conn))]
      (->> dim->actions
           (map (fn [[dim actions]]
                  [dim
                   (reduce #(expand-action (assoc opts :dim dim) %1 %2)
                           #{}
                           actions)]))
           (remove (fn [[_ concepts]] (empty? concepts)))
           (into {})))))
