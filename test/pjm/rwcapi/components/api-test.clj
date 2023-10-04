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
                                     :port )
             path]))

;; -------------------------------------------------------
;; Test 
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




;; -------------------------------------------------------
;; Test content ngotiation
;; -------------------------------------------------------
(deftest content-negotiation-test
  (testing " that text/html is not accepted and returns with status 406"
    (with-system [sut (core/rwcapi-system {:webserver {:port (get-free-port)}})]
      (is (= {:body "Not Acceptable"
              :status 406}
             (-> (sut->url sut
                           (url-for :greet))
                 (client/get {:accept :edn
                              :throw-exceptions false})
                 (select-keys [:body :status]))))))
  
  (testing "Content-negotiation that that application/edn is not accepted and returns with status 406"
    (with-system [sut (core/rwcapi-system {:webserver {:port (get-free-port)}})]
      (is (= {:body "Not Acceptable"
              :status 406}
             (-> (sut->url sut
                           (url-for :greet))
                 (client/get {:accept :html
                              :throw-exceptions false})
                 (select-keys [:body :status]))))))

  
  (testing "Content-negotiation that only application/json returns status 200"
    (with-system [sut (core/rwcapi-system {:webserver {:port (get-free-port)}})]
      (is (= {:body "Hello service - Pedestal component"
              :status 200}
             (-> (sut->url sut
                           (url-for :greet))
                 (client/get {:accept :json})
                 (select-keys [:body :status])))))))

;; -------------------------------------------------------
;; Test the echo endpoint
;; -------------------------------------------------------
(deftest echo-test
  (testing "Echo enpoint must return the request in the body"
    (with-system [sut (core/rwcapi-system {:webserver {:port (get-free-port)}})]
      (is (= {#_:body
              :status 200}
             (-> (sut->url sut
                           (url-for :echo))
                 (client/get)
                 (select-keys [#_:body :status])))))))

;; -------------------------------------------------------
;; Test the greet service
;; -------------------------------------------------------
(deftest greeting-test
  (testing "Greeting API must respond with a message"
    (with-system [sut (core/rwcapi-system {:webserver {:port (get-free-port)}})]
      (is (= {:body "Hello service - Pedestal component"
              :status 200}
             (-> (sut->url sut
                           (url-for :greet))
                 (client/get {:accept :json
                              :throw-exceptions false})
                 (select-keys [:body :status])))))))


;; -------------------------------------------------------
;; Test get-todo function.
;; -------------------------------------------------------
(deftest get-todo-test
  (let [todo-id1 (.toString (random-uuid))
        todo1 {:id todo-id1
               :name "My Todos"
               :items [{:id (.toString (random-uuid))
                        :name "Buy mil"}]}
        port (get-free-port)]
    
    (with-system [sut (core/rwcapi-system {:webserver {:port port}})] 
      (reset! (-> sut :in-memory-db-component :state-atom)
              [todo1])

      (testing "A known ID must return the expected TODO item"
        (is (= {:body todo1
                :status 200}
               (-> (sut->url sut
                             (url-for :get-todo
                                      {:path-params {:todo-id todo-id1}}))
                   (client/get {:accept :json
                                :as :json
                                :throw-exceptions false})
                   (select-keys [:body :status])))))

      (testing "Random ID should return a 404 and an empty body"
        (is (= {:body ""
                :status 404}
               (-> (sut->url sut
                             (url-for :get-todo
                                      {:path-params {:todo-id (.toString (random-uuid))}}))
                 (client/get {:throw-exceptions false})
                 (select-keys [:body :status]))))))))                           

;; -------------------------------------------------------
;; Test save-todo function.
;; -------------------------------------------------------
(deftest post-todo-test
  (let [todo-id (.toString (random-uuid))
        todo {:id todo-id
               :name "My Todos"
               :items [{:id (.toString (random-uuid))
                        :name "Buy milk"
                        :state "new"}]}
        port (get-free-port)]
    
    (with-system [sut (core/rwcapi-system {:webserver {:port port}})] 
     
      (testing "An item posted for saving must be returned"
        (is (= {:body todo
                :status 201}
               (-> (sut->url sut
                             (url-for :post-todo))
                   (client/post {:accept :json
                                 :content-type :application/json
                                 :as :json
                                 :throw-exceptions false
                                 :body (json/encode todo)})
                   (select-keys [:body :status]))))))))                           
