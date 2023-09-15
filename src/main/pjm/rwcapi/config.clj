(ns pjm.rwcapi.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]))



;;------------------------------------------------
;; The configuration is in the file config.edn in the
;; root dir
;;------------------------------------------------
(defn read-appconfig
  "Reads the resources/config.edn file from the src path"
  []
  (-> "config.edn"
      (io/resource)
      (aero/read-config)))
