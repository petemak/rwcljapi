(ns dev
  (:require [com.stuartsierra.component.repl :as comp-repl]
            [pjm.rwcapi.core :as core]))

;; ----------------------------------------------------------
;; From the documentaion ...
;; 
;; Provide an initializer function to com.stuartsierra.component.repl/set-init
;; to construct a Component system map. The initializer function takes one argument,
;; the old system, usually ignored.
;;
;; HikariCP requires :username instead of :user in the db-spec:
;; (def ^:private db-spec {:dbtype "..." :dbname "..." :username "..." :password "..."})
;; Passing the o
;; ----------------------------------------------------------

(comp-repl/set-init
 (fn [old-system] 
   (core/start-rwcapi-system {:webserver {:port 8081}
                              :db-spec    {:jdbcurl  "jdbc:postgresql://localhost:5432/rwcapi"
                                           :dbtype   "postgres"
                                           :dbname   "rwcapi"
                                           :username "rwcapi"
                                           :password  "rwcapi"}})))


;; ----------------------------------------------------------
;; cause system to restart
;; ----------------------------------------------------------
(defn reset-system!
  []
  (comp-repl/reset))
