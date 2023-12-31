(ns pjm.rwcapi.components.pedestal-component-test
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
;; Basic functionality tests 
;; -------------------------------------------------------
(deftest basic-test
    
  (testing "Correct generation of URLs for greet API from route names"
    (is (= "/echo" (url-for :echo))))

  (testing "Correct generation of URLs for TODO API by ID"
    (let [todo-id (random-uuid)]
      (is (=  (str "/todo/" todo-id) (url-for :get-todo :path-params {:todo-id todo-id})))))

  
  (testing "Unknown route should return an empty string"
    (is (thrown? clojure.lang.ExceptionInfo (url-for :bla-bla))))

  (testing "A valid free port must be found "
    (is (< 1 (get-free-port))))
  
  (testing "sut->url should return a correct URL"
    (let [port (get-free-port)]
      (with-system  [sut (core/start-rwcapi-system {:webserver {:port port}
                                                    :db-spec  {:jdbcurl  "jdbc:postgresql://localhost:5432/rwcapi"
                                                               :dbtype   "postgres"
                                                               :dbname   "rwcapi"
                                                               :username "rwcapi"
                                                               :password  "rwcapi"}})]
        (is (= (str "http://localhost:" port "/echo")
               (sut->url sut
                         (url-for :echo))))))))




;; -------------------------------------------------------
;; Test content ngotiation
;; -------------------------------------------------------
(deftest content-negotiation-test
  (testing "Content-negotiation that application/edn is not accepted and returns with status 406"
    (with-system [sut (core/start-rwcapi-system {:webserver {:port (get-free-port)}
                                                 :db-spec   {:jdbcurl  "jdbc:postgresql://localhost:5432/rwcapi"
                                                             :dbtype   "postgres"
                                                             :dbname   "rwcapi"
                                                             :username "rwcapi"
                                                             :password  "rwcapi"}})]
      (is (= {:body "Not Acceptable"
              :status 406}
             (-> (sut->url sut
                           (url-for :echo))
                 (client/get {:accept :edn
                              :throw-exceptions false})
                 (select-keys [:body :status]))))))
  
  (testing "Content-negotiong that application/html is not accepted and returns with status 406"
    (with-system [sut (core/start-rwcapi-system {:webserver {:port (get-free-port)}
                                                 :db-spec   {:jdbcurl  "jdbc:postgresql://localhost:5432/rwcapi"
                                                                        :dbtype   "postgres"
                                                                        :dbname   "rwcapi"
                                                                        :username "rwcapi"
                                                                        :password  "rwcapi"}})]
      (is (= {:body "Not Acceptable"
              :status 406}
             (-> (sut->url sut
                           (url-for :echo))
                 (client/get {:accept :html
                              :throw-exceptions false})
                 (select-keys [:body :status]))))))

  
  (testing "Content-negotiation that only application/json returns status 200"
    (with-system [sut (core/start-rwcapi-system {:webserver {:port (get-free-port)}
                                                 :db-spec   {:jdbcurl  "jdbc:postgresql://localhost:5432/rwcapi"
                                                             :dbtype   "postgres"
                                                             :dbname   "rwcapi"
                                                             :username "rwcapi"
                                                             :password  "rwcapi"}})]
      (is (= {:status 200}
             (-> (sut->url sut
                           (url-for :echo))
                 (client/get {:accept :json})
                 (select-keys [:status])))))))

;; -------------------------------------------------------
;; Test the echo endpoint
;; -------------------------------------------------------
(deftest echo-test
  (testing "Info endpoint must return service information"
    (with-system [sut (core/start-rwcapi-system {:webserver {:port (get-free-port)}
                                                 :db-spec   {:jdbcurl  "jdbc:postgresql://localhost:5432/rwcapi"
                                                             :dbtype   "postgres"
                                                             :dbname   "rwcapi"
                                                             :username "rwcapi"
                                                             :password  "rwcapi"}})]
      (is (= {#_:body
              :status 200}
             (-> (sut->url sut
                           (url-for :echo))
                 (client/get)
                 (select-keys [#_:body :status])))))))



;; -------------------------------------------------------
;; Test the echo endpoint
;; -------------------------------------------------------
(deftest info-test
  (testing "Echo enpoint must return the request in the body"
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


;; -------------------------------------------------------
;; Test the greet service
;; -------------------------------------------------------
(comment

  
  (deftest greeting-test
    (testing "Greeting API must respond with a message"
      (with-system [sut (core/start-rwcapi-system {:webserver {:port (get-free-port)}
                                                   :db-spec   {:jdbcurl  "jdbc:postgresql://localhost:5432/rwcapi"
                                                               :dbtype   "postgres"
                                                               :dbname   "rwcapi"
                                                               :username "rwcapi"
                                                               :password  "rwcapi"}})]
        (is (= {
                :status 200}
               (-> (sut->url sut
                             (url-for :echo))
                   (client/get {:accept :json
                                :throw-exceptions false})
                   (select-keys [:status]))))))))


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
    
    (with-system [sut (core/start-rwcapi-system {:webserver {:port port}
                                                 :db-spec   {:jdbcurl  "jdbc:postgresql://localhost:5432/rwcapi"
                                                             :dbtype   "postgres"
                                                             :dbname   "rwcapi"
                                                             :username "rwcapi"
                                                             :password  "rwcapi"}})] 
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
                        :status "new"
                        :description "From Migros, get the Zuri brand"}]}
        port (get-free-port)]
    
    (with-system [sut (core/start-rwcapi-system {:webserver {:port port}
                                                 :db-spec  {:jdbcurl  "jdbc:postgresql://localhost:5432/rwcapi"
                                                            :dbtype   "postgres"
                                                            :dbname   "rwcapi"
                                                            :username "rwcapi"
                                                            :password  "rwcapi"}})] 
     
      (testing "A valid TODO posted for saving must be returned"
        (is (= {:body todo
                :status 201}
               (-> (sut->url sut
                             (url-for :post-todo))
                   (client/post {:accept :json
                                 :content-type :application/json
                                 :as :json
                                 :throw-exceptions false
                                 :body (json/encode todo)})
                   (select-keys [:body :status])))))


      (testing "A valid TODO, once posted for saving, can be retrieved by ID"
        (is (= {:body todo
                :status 200}
               (-> (sut->url sut
                             (url-for :get-todo
                                      {:path-params {:todo-id todo-id}}))
                   (client/get {:accept :json
                                :as :json
                                :throw-exceptions false})
                   (select-keys [:body :status])))))

      
      (testing "An invalid TODO should ned with an HTTP 500"
        (is (= {:status 500}
               (-> (sut->url sut
                             (url-for :post-todo))
                   (client/post {:accept :json
                                 :content-type :application/json
                                 :as :json
                                 :throw-exceptions false
                                 :body (json/encode {:id "werwerewr"
                                                     :name "Bla bla"})})
                   (select-keys [:status]))))))))                           
