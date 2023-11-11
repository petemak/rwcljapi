(ns pjm.rwcapi.core
  (:require [pjm.rwcapi.config :as cfg]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [pjm.rwcapi.components.sample-component :as sample-comp]
            [pjm.rwcapi.components.pedestal-component :as pedestal-comp]
            [pjm.rwcapi.components.in-memory-db-component :as in-mem-db-comp]
            [next.jdbc.connection :as next-jdbc-connection])
  (:import (com.zaxxer.hikari HikariDataSource)
           (org.flywaydb.core Flyway)))

;; ---------------------------------------------
;; Used as init-fn function to run migrstion
;; ---------------------------------------------
(defn migrate-todo-schema
  "Load schema deifinition and run against connection"
  [datasource]
  (log/info "::=> migrate-todo-schema - Running database init")
  (.migrate
   (.. (Flyway/configure)
       (dataSource datasource)
       ; https://www.red-gate.com/blog/database-devops/flyway-naming-patterns-matter
       (locations (into-array String ["classpath:database/migrations"]))
       (table "schema_version")
       (load))))
;; ---------------------------------------------
;; Component system
;;
;; sample-component <- data-source<-  pedestal-component
;;
;; Note the next-jdbconnection component function
;; will run the :init-fn, if found,  function passing the datasource
;;
;; If db-spec contains :init-fn, that is assumed to be
;; a function that should be called on the newly-created datasource.
;;
;; See: https://cljdoc.org/d/com.github.seancorfield/next.jdbc/1.3.894/api/next.jdbc.connection#component
;; This allows for modification of (mutable) connection
;; pooled datasource and/or some sort of database initialization/setup
;; to be called automatically.
;; ---------------------------------------------
(defn start-rwcapi-system
  "Creates a system map from the specified configuration
   ready for starting and returns it"
  [cfg]
  (component/system-map
   :sample-component (sample-comp/new-sample-component cfg)
   :data-source (next-jdbc-connection/component
                 HikariDataSource
                 (assoc (:db-spec cfg) :init-fn migrate-todo-schema)
                 )
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
  (log/info "::-> Starting RWCAPI services with config: " (cfg/read-appconfig) "....")  
  (let [system (-> (cfg/read-appconfig)
                   (start-rwcapi-system)
                   (component/start-system))]

    (add-shutdown-hook system)))
