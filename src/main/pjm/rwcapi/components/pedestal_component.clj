(ns pjm.rwcapi.components.pedestal-component
  (:require [com.stuartsierra.component :as comp]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]))





;; -------------------------------------------------------
;; Utility fuctions
;; -------------------------------------------------------
(defn response 
  "Retunrs a basic response map contructed from the
  specified body and status"
  [status body]
  {:status status
   :body body
   :headers nil})

(def ok       (partial response 200))
(def created  (partial response 201))


(defn get-todo-by-id
  "Returns the TODO with specified ID"
  [{:keys [in-memory-db-component]} todo-id]
  (println "::-> get-to-do-by-id:-[" in-memory-db-component "]-")
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
;; TO DO handler.Returns a lambda that reads the
;; ID of a TODO from the request path-params and
;; calls get-todo-ny id()
;; ---------------------------------------------
(def get-todo-handler
  {:name :get-todo-handler
   :enter
   (fn [{:keys [dependencies] :as context}]
     (println "::-> go-to-handler - keys: " (keys context))
     (let [request (:request context)
           response (ok (get-todo-by-id dependencies
                                        (-> request
                                            :path-params
                                            :todo-id
                                            (parse-uuid))))]
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
;; In Pestal routing is the process
;; of matching an incoming request
;; to a handler or chain of handlers.
;; ---------------------------------------------
(def routes
  (route/expand-routes                                   
   #{["/greet"         :get hello-handler    :route-name :greet]
     ["/todo/:todo-id" :get get-todo-handler :route-name :get-todo]}))

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
                     (update ::http/interceptors concat [(inject-dependencies-interceptor component)] )
                     (http/create-server)
                     (http/start))]
      (assoc component :server server)))

  (stop [component]
    (println ":: => PedestalComponent -  stop: component" )
    (when-let [server (:server component)]
      (http/stop server))
    (assoc component :server nil)))


(defn new-pedestal-component
  [config]
  (map->PedestalComponent {:config config}))
