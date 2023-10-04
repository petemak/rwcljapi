(ns pjm.rwcapi.components.pedestal-component
  (:require [com.stuartsierra.component :as comp]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.content-negotiation :as content-ngotiation]
            [cheshire.core :as json]))


;; --------------------------------------------- [cheshire.core :refer :all]
;; Intercpetor verfifies that only supported content-types
;; are accepted. Fails first with an HTTP 406
;; ---------------------------------------------
(def supported-types [#_"text/html" #_"application/edn" "application/json" #_"text/plain"]) 
(def content-negotiation-interceptor (content-ngotiation/negotiate-content supported-types))


;; -------------------------------------------------------
;; Utility fuctions
;; -------------------------------------------------------
(defn response 
  "Retunrs a basic response map contructed from the
  specified body and status"
  ([status]
   (response status nil))
  ([status body]
   (merge 
    {:status status
     :headers {"Content-Type" "application/json"}}
    (when body {:body body}))))

(def ok        (partial response 200))
(def created   (partial response 201))
(def not-found (partial response 404))


(defn get-todo-by-id
  "Returns the TODO with specified ID"
  [{:keys [in-memory-db-component]} todo-id]
  (->> @(:state-atom in-memory-db-component)
       (filter (fn [todo]
               (= todo-id (:id todo))))
       (first)))



;; ---------------------------------------------
;; Hello world handler
;; ---------------------------------------------
(defn hello-handler
  "Respons with Hello world!"
  [request]          
  {:status 200
   :body "Hello service - Pedestal component"})



;; ---------------------------------------------
;; Echo interceptor extracts the request and adds
;; it as the the body of the response 
;; ---------------------------------------------
(def echo-intereptor
  {:name ::echo                                                                   
   :enter (fn [context]                                                           
            (let [request (:request context)                                      
                  response (ok request)]                                          
              (assoc context :response response)))})

;; ---------------------------------------------
;; The simplest way to create an interceptor
;; is by just making a map. Any map that has
;; at least one key of :enter, :leave, or
;; :error can be used as an interceptor.
;; ---------------------------------------------
(defn inject-dependencies-interceptor
  "Injects depencies into the context map"
  [dependencies]
  (interceptor/interceptor
   {:name ::inject-dependencies-interceptor
    :enter (fn [context]
             (assoc context :dependencies dependencies))}))



;; ---------------------------------------------
;; TO DO GET handler.Returns a lambda that reads the
;; ID of a TODO from the request path-params and
;; calls get-todo-ny id()
;; ---------------------------------------------
(def get-todo-interceptor
  {:name :get-todo-interceptor
   :enter
   (fn [{:keys [dependencies] :as context}]
     (let [request (:request context)
           todo    (get-todo-by-id dependencies
                                   (-> request
                                       :path-params
                                       :todo-id))
           response (if todo
                      (ok (json/encode todo))
                      (not-found))]
       (assoc context :response response)))})


;; ---------------------------------------------
;; Save a TODOD to the internal in memory cache
;; ---------------------------------------------
(defn save-todo!
  "Saves a TODO with specified ID"
  [{:keys [in-memory-db-component]} todo]
  (println "::-> save-todo!:-[" todo "]-")
  (swap! (:state-atom in-memory-db-component) conj todo))


;; ---------------------------------------------
;; TO DO POST handler.Returns a lambda that reads the
;; ID of a TODO from the request path-params and
;; calls get-todo-ny id()
;; ---------------------------------------------
(def post-todo-interceptor
  {:name :post-todo-interceptor
   :enter
   (fn [{:keys [dependencies] :as context}]
     (let [request (:request context)
           todo    (:json-params request) ]
       (save-todo! dependencies todo)
       (assoc context :response (created (json/encode todo)))))})


;; ---------------------------------------------
;; In Pestal routing is the process
;; of matching an incoming request
;; to a handler or chain of handlers.
;; ---------------------------------------------
(def routes
  (route/expand-routes                                   
   #{["/echo"          :get  echo-intereptor       :route-name :echo]
     ["/greet"         :get  hello-handler         :route-name :greet]
     ["/todo/:todo-id" :get  get-todo-interceptor  :route-name :get-todo]
     ["/todo"          :post [(body-params/body-params) post-todo-interceptor] :route-name :post-todo]}))

;; -------------------------------------------------------
;; Utility function for generating routes URLS from route names. The returned
;; function takes a route and returns a URL
;; -------------------------------------------------------
(def url-for
  (route/url-for-routes routes))


;; ------------------------------------------------
;; Expects a configuration map and reference to the 
;; sample compoent as a dependency
;; -------------------------------------------------
(defrecord PedestalComponent
    [config example-component in-memory-db-component]
  
  ;; Implement the Lifecycle protocol
  comp/Lifecycle

  ;; Creates a pedestal service ma p and starts it
  ;; with io.pedestal.http/start
  (start [component]
    (println "::=> PedestalComponent - start - " component)
    (let [server (-> {::http/routes routes  
                      ::http/type   :jetty
                      ::http/join?  false
                      ::http/port   (-> config :webserver :port)}
                     (http/default-interceptors)
                     (update ::http/interceptors concat
                             [(inject-dependencies-interceptor component)
                              content-negotiation-interceptor] )
                     (http/create-server)
                     (http/start))]
      (assoc component :server server)))

  (stop [component]
    (println ":: => PedestalComponent -  stop: component" )
    (when-let [server (:server component)]
      (http/stop server))
    (assoc component :server nil)))


;; ------------------------------------------------
;; Create a pedestal component 
;; -------------------------------------------------
(defn new-pedestal-component
  [config]
  (map->PedestalComponent {:config config}))

