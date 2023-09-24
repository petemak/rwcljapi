(ns pjm.rwcapi.core
  (:require [pjm.rwcapi.config :as cfg]
            [com.stuartsierra.component :as component]
            [pjm.rwcapi.components.sample-component :as sample-comp]
            [pjm.rwcapi.components.pedestal-component :as pedestal-comp]))




;; ---------------------------------------------
;; Component system
;;
;; sample-component <- pedestal-component
;; ---------------------------------------------
(defn rwcapi-system
  "Creates a system map from the specified configuration
   ready for starting and returns it"
  [cfg]
  (component/system-map
   :sample-component (sample-comp/new-sample-component cfg)
   :pedestal-component (component/using (pedestal-comp/new-pedestal-component cfg)
                                        [:sample-component])))

;; ---------------------------------------------
;; Stopping the system when the app shuts down
;; ---------------------------------------------
(defn add-shutdown-hook
  "Add shutdown hook to stop specified system"
  [system]
  (.addShutdownHook
   (Runtime/getRuntime)
   (new Thread #(component/stop-system system))))

;; ---------------------------------------------
;; Main entry function
;; ---------------------------------------------
(defn main
  [args]
  (println "Starting RWCAPI services with config: " (cfg/read-appconfig) "....")  
  (let [system (-> (cfg/read-appconfig)
                   (rwcapi-system)
                   (component/start-system))]

    (println "Started RWCAPI services with system map: " system)

    (add-shutdown-hook system)))
