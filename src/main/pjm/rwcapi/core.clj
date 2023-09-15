(ns pjm.rwcapi.core
  (:require [pjm.rwcapi.config :as cfg]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]))


;; ---------------------------------------------
;; Hello world handler
;; ---------------------------------------------
(defn respond-hello
  "Respons with Hello world!"
  [request]          
  {:status 200
   :body "Hello, world!"})


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
(defn create-server
  "Creates a service map ready to be started
   with io.pedestal.http/start"
  [port]
  (http/create-server     
   {::http/routes routes  
    ::http/type   :jetty  
    ::http/port   port})) 


;; ---------------------------------------------
;; Main entry function
;; ---------------------------------------------
(defn main
  [args]
  (let [cfg     (cfg/read-appconfig)
        port    (-> cfg :webserver :port)
        svc-map (create-server 8890)]

    (println "Staring application with configuration:" cfg)
    (http/start svc-map)))
