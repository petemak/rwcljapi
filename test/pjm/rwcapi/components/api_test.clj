(ns pjm.rwcapi.components.api-test
  (:require [clojure.test :refer :all]
            [pjm.rwcapi.core :as core]
            [clj-http.client :as client]
            [com.stuartsierra.component :as component]
            [pjm.rwcapi.components.pedestal-component :refer [url-for]]
            [clojure.string :as str]
            [cheshire.core :as json]))




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
;; Help function
;; -------------------------------------------------------
(defn get-free-port
  "Retunrs a local port that is free"
  []
  (with-open [socket (java.net.ServerSocket. 0)]
    (.getLocalPort socket)))


;; -------------------------------------------------------
;;
;; -------------------------------------------------------
(defn sut->url
  "General a URL for the system under test for an endpoint with the given path"
  [sut path]
  (str/join ["http://localhost:" (-> sut
                                     :pedestal-component
                                     :config
                                     :webserver
                                     :port)
             path]))

;; -------------------------------------------------------
;; Basic tests 
;; -------------------------------------------------------
(deftest basic-test
  (testing "A valid free port must be found "
    (is (< 1 (get-free-port))))
  
  (testing "System under test sut->url should return a correct URL"
    (let [port (get-free-port)]
      (with-system  [sut (core/start-rwcapi-system {:webserver {:port port}
                                                    :db-spec    {:jdbcurl  "jdbc:postgresql://localhost:5432/rwcapi"
                                                                 :dbtype   "postgres"
                                                                 :dbname   "rwcapi"
                                                                 :username "rwcapi"
                                                                 :password  "rwcapi"}})]
        (is (= (str "http://localhost:" port "/echo")
               (sut->url sut
                         (url-for :echo))))))))


;; -------------------------------------------------------
;; Test the info endpoint
;; -------------------------------------------------------
(deftest info-test
  (testing "Info endpoint must return the request in the body"
    (with-system [sut (core/start-rwcapi-system {:webserver {:port (get-free-port)}
                                                 :db-spec   {:jdbcurl  "jdbc:postgresql://localhost:5432/rwcapi"
                                                             :dbtype   "postgres"
                                                             :dbname   "rwcapi"
                                                             :username "rwcapi"
                                                             :password  "rwcapi"}})]
      (is (= {:body "\"15.4 (Debian 15.4-2.pgdg120+1)\""
              :status 200}
             (-> (sut->url sut
                           (url-for :info))
                 (client/get {:accept :json
                              :throw-exceptions false})
                 (select-keys [:body :status])))))))
