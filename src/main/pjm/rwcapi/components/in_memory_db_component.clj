(ns pjm.rwcapi.components.in-memory-db-component
  (:require [com.stuartsierra.component :as comp]))


(comment
  [{:id (random-uuid)
    :name "My TODO list"
    :items [{:id (random-uuid)
             :name "Buy milk"
             :status :created}]}
   {:id (random-uuid)
    :name "My other TODO list"
    :items []}   ]
  )

(defrecord InMemoryDBComponent
    [config]
  ;; Implement the Lifecycle protocol
  comp/Lifecycle

  (start [component]
    (println "::=> InMemoryDBComponent - start: -[" component "]-")
    (assoc component :state-atom (atom [])))

  (stop [component]
    (println ":: => InMemoryDBComponent - stop: -[" component "]-")
    (assoc component :state-atom nil)))


(defn new-in-memory-db-component
  [cfg]
  (map->InMemoryDBComponent {:config cfg}))
