(ns pjm.rwcapi.persistence.db-test
  (:require [pjm.rwcapi.core :as rwcore]
            [clojure.test :refer :all]
            [clj-test-containers.core :as tc]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [clj-test-containers.core :as tc]
            [com.stuartsierra.component :as component]))


;; -------------------------------------------------------
;; The with-system macro allows us to start/stop systems
;; between test executions.
;; -------------------------------------------------------
(defmacro with-system
  [[bound-var binding-expr] & body]
  `(let [~bound-var (component/start ~binding-expr)]
     (try
       ~@body
       (finally
         (component/stop ~bound-var)))))


;; -------------------------------------------------------
;; We want a systen containing only the database
;; -------------------------------------------------------
(defn datasource-only-system
  "Create system containing only a datasurce"
  [cfg]
  (component/system-map
    :data-source (rwcore/datasource-component cfg)))


;; -------------------------------------------------------
;; Test loading Postgres DB testcontainer and
;; executing a SELECT command
;;
;; Required 
;;  - docker service must be running:
;;    sudo systemctl start docker.service 
;; -------------------------------------------------------
(deftest db-testcontainer-test
  (testing "Loding PostgreSQL test container and running a version check must work"
    (let [pw "db-pass"
          postgres (-> (tc/create {:image-name    "postgres:15.4"
                                   :exposed-ports [5432]
                                   :env-vars      {"POSTGRES_PASSWORD" pw}})
                       (tc/bind-filesystem! {:host-path      "/tmp"
                                             :container-path "/opt"
                                             :mode           :read-only})
                       (tc/start!))]

      (try
        (let [datasource (jdbc/get-datasource {:dbtype   "postgresql"
                                               :dbname   "postgres"
                                               :user     "postgres"
                                               :password pw
                                               :host     (:host postgres)
                                               :port     (get (:mapped-ports postgres) 5432)})]


          (is (= "localhost" (:host postgres)))
          (is (= [{:server_version "15.4 (Debian 15.4-2.pgdg120+1)"}]
                 (jdbc/execute! datasource ["SHOW server_version;"])))
          
          (is (= [{:x 1}]
                 (with-open [connection (jdbc/get-connection datasource)]
                   (jdbc/execute! connection ["SELECT 1 as x;"])))))
        (finally
          (tc/stop! postgres))))))


; -------------------------------------------------------
; test migration
; -------------------------------------------------------
(deftest db-migration-test
  (testing "Migrations must create schema and seed tables for todos"
    (let [pw "rwcapi"
          container (-> (tc/create {:image-name    "postgres:15.4"
                                    :exposed-ports [5432]
                                    :env-vars      {"POSTGRES_PASSWORD" pw}})
                        (tc/bind-filesystem! {:host-path      "/tmp"
                                              :container-path "/opt"
                                              :mode           :read-only})
                        (tc/start!))]


      ;; (is (=  {} container))
      (is (= [5432] (:exposed-ports container) ))
      (is (= 5432 (-> (:exposed-ports container)
                   first) ))

      
      (try
        (with-system [sut (datasource-only-system {:db-spec {:dbtype   "postgresql"
                                                            :dbname   "rwcapi"
                                                            :username "rwcapi"
                                                            :password "rwcapi"
                                                            :host     (:host container)
                                                            :port     (-> (:exposed-ports container)
                                                                          first)}})]


         ;; sut is a map with fubction {:data-source #function[clojure.lang.AFunction/1]}	  
         (let [{:keys [data-source]} sut
               schema-version  (jdbc/execute! (data-source)
                                              ["select * from schema_version"]
                                              {:builder-fn rs/as-unqualified-lower-maps})]
              ;; NOTE: schema-version is
              ;; a vector containng a map [{:desr...}]
              (is (= 1 (count schema-version)))
              (is (= {:description "add todo tables"
                      :script      "V1__add_todo_tables.sql"
                      :version     "1"
                      :success     true}
                  (select-keys (first schema-version) [:description :script :version :success])))))

        (finally
          (tc/stop! container))))))

; -------------------------------------------------------
; test insert
; -------------------------------------------------------
(deftest db-insert-test
  (testing "Inserts must create todos"
    (let [pw "rwcapi"
          container (-> (tc/create {:image-name    "postgres:15.4"
                                    :exposed-ports [5432]
                                    :env-vars      {"POSTGRES_PASSWORD" pw}})
                        (tc/bind-filesystem! {:host-path      "/tmp"
                                              :container-path "/opt"
                                              :mode           :read-only})
                        (tc/start!))]


      (try
        (with-system [sut (datasource-only-system {:db-spec {:dbtype   "postgresql"
                                                            :dbname   "rwcapi"
                                                            :username "rwcapi"
                                                            :password "rwcapi"
                                                            :host     (:host container)
                                                            :port     (-> (:exposed-ports container)
                                                                          first)}})]


          ;; sut is a map with fubction {:data-source #function[clojure.lang.AFunction/1]}	  
          (let [{:keys [data-source]} sut
                insert-results (jdbc/execute! (data-source)
                                              ["INSERT INTO todo (title, description)
                                                VALUES ('Work', 'bbabba'), ('Learn', 'jjjjj') returning *"]
                                             {:builder-fn rs/as-unqualified-lower-maps})
                select-results (jdbc/execute! (data-source)
                                             ["select * from todo"]
                                             {:builder-fn rs/as-unqualified-lower-maps})]

            (is (= 2 (count insert-results)
                     (count select-results)))))

        (finally
          (tc/stop! container))))))



