(ns user
  (:require [com.stuartsierra.component.repl :as comp-repl]
            [pjm.rwcapi.core :as core]))

;; ----------------------------------------------------------
;; From the documentaion ...
;; 
;; Provide an initializer function to com.stuartsierra.component.repl/set-init
;; to construct a Component system map. The initializer function takes one argument,
;; the old system, usually ignored.
;;
;; Passing the o
;; ----------------------------------------------------------

(comp-repl/set-init
 (fn [old-system] 
   (core/rwcapi-system {:webserver {:port 8081}})))
