(ns environment.core
  (:require
   [clojure.java.io :as jio]
   [integrant.core :as ig]
   )
  (:import
   sun.misc.Signal
   sun.misc.SignalHandler
   ))


;; * Signal


;; graalvm native-image


(def ^Signal SIGINT (Signal. "INT"))
(def ^Signal SIGTERM (Signal. "TERM"))


(defn signal-handler
  ^SignalHandler
  [f]
  (proxy [SignalHandler] []
    (handle [sig] (f sig))))


(defn install-system-exit-signal-handler!
  "https://github.com/oracle/graal/issues/465"
  []
  ;; handle Ctrl-C interrupt -> System/exit -> run shutdown hook
  (Signal/handle SIGINT (signal-handler (fn [_] (System/exit 0))))


  ;; handle process term sig
  (Signal/handle SIGTERM (signal-handler (fn [_] (System/exit 0))))


  (println "Signal handler registered."))


;; * Profiled


(def ^:dynamic *profile* (or (keyword (System/getProperty "environment.profile")) :dev))


(defn elide-profile-code?
  [coll]
  (not
    (contains? (set coll) *profile*)))


(defmacro profiled
  [coll & body]
  (when-not (elide-profile-code? coll)
    `(do ~@body)))


;; * dynamic variables for init-time


(def ^:dynamic *volume-directory* (System/getProperty "environment.volume.directory"))


(defn file-or-resource
  "CAUTION: Don't use this fn in runtime, only use in init-time."
  [path]
  (let [file     (jio/file *volume-directory* path)
        resource (jio/resource path)]
    (cond
      (.isFile file) file
      :else          resource)))


;; * properties


(defn set-properties!
  "init-time fn"
  [{:keys [volume-dir]}]
  (when (string? volume-dir) (System/setProperty "environment.volume.directory" volume-dir)))
