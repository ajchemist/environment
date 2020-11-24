(ns environment.server-render-test
  (:require
   [clojure.java.io :as jio]
   [clojure.test :as test :refer [deftest is are testing]]
   [integrant.core :as ig]
   [environment.server-render :as env.server-render]
   ))


(def watch-dir (doto (jio/file "server-render-test") (.mkdir)))


(def file-source (jio/file watch-dir "server-render.edn"))


(def system-map
  (-> (env.server-render/system-map {:watch-dir watch-dir})
    #_(assoc-in [::env.server-render/file-source-watcher-args :callback]
      (fn [reference _ _]
        (is (= :y (:x @reference)))))))


(def system (-> system-map (ig/prep) (ig/init)))


(deftest main
  (jio/delete-file file-source true)
  (is (empty? @(val (ig/find-derived-1 system ::env.server-render/reference))))
  (spit file-source "{:x :y}")
  (Thread/sleep 2000)
  (is (= :y (:x @(val (ig/find-derived-1 system ::env.server-render/reference))))))
