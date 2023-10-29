(ns pjm.rwcapi.core
  (:require [pjm.rwcapi.config :as cfg]
            [com.stuartsierra.component :as component]
            [pjm.rwcapi.components.sample-component :as sample-comp]
            [pjm.rwcapi.components.pedestal-component :as pedestal-comp]
            [pjm.rwcapi.components.in-memory-db-component :as in-mem-db-comp]
            [next.jdbc.connection :as next-jdbc-connection])
  (:import (com.zaxxer.hikari HikariDataSource)))

;; ---------------------------------------------
;; Component system
;;
;; sample-component <- data-source<-  pedestal-component
;; ---------------------------------------------

(defn start-rwcapi-system
  "Creates a system map from the specified configuration
   ready for starting and returns it"
  [cfg]
  (component/system-map
   :sample-component (sample-comp/new-sample-component cfg)
   :data-source (next-jdbc-connection/component HikariDataSource (:db-spec cfg))
   :in-memory-db-component (in-mem-db-comp/new-in-memory-db-component cfg)
   :pedestal-component (component/using (pedestal-comp/new-pedestal-component cfg)
                                        [:sample-component :data-source :in-memory-db-component])))

;; ---------------------------------------------
;; Stopping the system when the app shuts down
;; ---------------------------------------------
(defn add-shutdown-hook
  "Add shutdown hook to stop specified system"
  [system]
  (.addShutdownHook
   (Runtime/getRuntime)
   (new Thread #(component/stop-system system))))

;; ---------------------------------------------
;; Main entry function
;; ---------------------------------------------
(defn main
  [args]
  (println "::-> Starting RWCAPI services with config: " (cfg/read-appconfig) "....")  
  (let [system (-> (cfg/read-appconfig)
                   (start-rwcapi-system)
                   (component/start-system))]

    (add-shutdown-hook system)))
