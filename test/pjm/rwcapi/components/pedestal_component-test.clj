(ns pjm.rwcapi.components.pedestal-component-test
  (:require [clojure.test :refer :all]
            [pjm.rwcapi.core :as core]
            [clj-http.client :as client]
            [com.stuartsierra.component :as component]
            [pjm.rwcapi.components.pedestal-component :as pedestal-component]))


;; -------------------------------------------------------
;; Check basics
;; -------------------------------------------------------
(deftest simple-test
  (is (= 2 2))) 

(deftest url-generation-test
  
  (testing "Correct generation of URLs for greet API from route names"
    (is (= "/greet" (pedestal-component/url-for :greet))))

  (testing "Correct generation of URLs for TODO API by ID"
    (let [todo-id (random-uuid)]
      (is (=  (str "/todo/" todo-id) (pedestal-component/url-for :get-todo :path-params {:todo-id todo-id})))))

  
  (testing "Unknown route should return an empty string"
    (is (thrown? clojure.lang.ExceptionInfo (pedestal-component/url-for :bla-bla)))))
