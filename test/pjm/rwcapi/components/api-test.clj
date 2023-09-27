(ns pjm.rwcapi.components.api-test
  (:require [clojure.test :refer :all]
            [pjm.rwcapi.core :as core]
            [clj-http.client :as client]
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
;;
;; -------------------------------------------------------
(deftest simple-test
  (is (= 2 2))) 

;; -------------------------------------------------------
;; Test the greet function.
;; -------------------------------------------------------
(deftest greeting-test
  (with-system [sut (core/rwcapi-system {:webserver {:port 8081}})]
    (is (= {:body "Hello service - Pedestal component"
            :status 200}
           (-> (str "http://localhost:" 8081 "/greet")
               (client/get)
               (select-keys [:body :status]))))))


;; --------------------
;; Test get-todo function.
;; -------------------------------------------------------
(deftest get-todo-test
  (let [todo-id1 (random-uuid)
        todo1 {:id todo-id1
               :name "My Todos"
               :items [{:id (random-uuid)
                        :name "Buy mil"}]}]
    
    (with-system [sut (core/rwcapi-system {:webserver {:port 8081}})] 
      (reset! (-> sut :in-memory-db-component :state-atom)
              [todo1])

      (testing "A known ID must return the expected TODo item"
        (is (= {:body (pr-str todo1)
                :status 200}
               (-> (str "http://localhost:" 8081 "/todo/" todo-id1)
                   (client/get)
                   (select-keys [:body :status])))))

      (testing "Random ID should return an empty body"
        (is (= {:body ""
                :status 200}
             (-> (str "http://localhost:" 8081 "/todo/" (random-uuid))
                 (client/get)
                 (select-keys [:body :status])))))
      )))                           

