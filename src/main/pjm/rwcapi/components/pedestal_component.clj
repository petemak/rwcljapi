(ns pjm.rwcapi.components.pedestal-component
  (:require [com.stuartsierra.component :as comp]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]))

;; ---------------------------------------------
;; Hello world handler
;; ---------------------------------------------
(defn respond-hello
  "Respons with Hello world!"
  [request]          
  {:status 200
   :body "Hello service - Pedestal component"})


;; ---------------------------------------------
;; In Pestal routing is the process
;; of matching an incoming request
;; to a handler or chain of handlers.
;; ---------------------------------------------
(def routes
  (route/expand-routes                                   
   #{["/greet" :get respond-hello :route-name :greet]}))


;; ---------------------------------------------
;; The sevrer or service definition
;; ---------------------------------------------




;; ------------------------------------------------
;; Expects a configuration map and reference to the 
;; sample compoent as a dependency
;; -------------------------------------------------
(defrecord PedestalComponent
    [config example-component]
  
  ;; Implement the Lifecycle protocol
  comp/Lifecycle

  ;; Creates a pedestal service ma p and starts it
  ;; with io.pedestal.http/start
  (start [component]
    (println "::=> PedestalComponent - start: -[" component "]-")
    (let [server (-> {::http/routes routes  
                      ::http/type   :jetty
                      ::http/join?  false
                      ::http/port   (-> config :webserver :port)}
                     (http/create-server)
                     (http/start))]
      (println "::=> PedestalComponent - start: service map started: -[" server "]-")
      (assoc component :server server)))

  (stop [component]
    (println ":: => PedestalComponent -  stop: component = -[" component "]-")
    (println ":: => PedestalComponent -  stop: server = -[" (:server component) "]-")    
    (when-let [server (:server component)]
      (http/stop server))
    (assoc component :server nil)))


(defn new-pedestal-component
  [config]
  (println ":: => new-pedestal-component() - creating pedestal component with config: " config)
  (map->PedestalComponent {:config config}))
