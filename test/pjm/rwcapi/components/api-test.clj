(ns pjm.rwcapi.components.api-test
  (:require [clojure.test :refer :all]
            [pjm.rwcapi.core :as core]
            [clj-http.client :as client]
            [com.stuartsierra.component :as component]
            [pjm.rwcapi.components.pedestal-component :refer [url-for]]
            [clojure.string :as str]
            [cheshire.core :as json]
            ))




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
  
  (testing "sut->url should return a correct URL"
    (let [port (get-free-port)]
      (with-system  [sut (core/rwcapi-system {:webserver {:port port}})]
        (is (= (str "http://localhost:" port "/greet")
               (sut->url sut
                         (url-for :greet))))))))


