(ns pjm.rwcapi.persistence.db-test
  (:require [clojure.test :refer :all]
            [clj-test-containers.core :as tc]
            [next.jdbc :as jdbc]
            [clj-test-containers.core :as tc]))


; -------------------------------------------------------
; Test loading Postgre DB testcontainer
; -------------------------------------------------------
(deftest db-integration-test
  (testing "A simple PostgreSQL integration test"
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
; test containers
; -------------------------------------------------------

