(ns pjm.rwcapi.components.sample-component
  (:require [com.stuartsierra.component :as comp]))

(defrecord SampleComponent [cfg]
  ;; Implement the Lifecycle protocol
  comp/Lifecycle

  (start [component]
    (println "::=> SampleComponent - start: -[" component "]-")
    (assoc component :state ::started))

  (stop [component]
    (println ":: => SampleComponent - stop: -[" component "]-")
    (assoc component :state nil)))


(defn new-sample-component
  [cfg]
  (map->SampleComponent {:config cfg}))
