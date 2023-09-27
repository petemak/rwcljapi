(ns unit.rwcapi.simple-test
  (:require [clojure.test :refer :all]
            [pjm.rwcapi.config]))

;;------------------------------------------------
;; Test check
;;------------------------------------------------
(deftest simple-test
  (is (= 1 1)))
