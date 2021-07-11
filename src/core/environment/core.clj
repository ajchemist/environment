(ns environment.core
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as jio]
   [integrant.core :as ig]
   [environment.merge :as env.merge]

   [environment.core :as env])
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


(def ^:dynamic *profile*
  (or
    (keyword (System/getProperty "environment.profile"))
    (keyword (System/getenv "ENVIRONMENT_PROFILE"))
    :dev))


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
  "CAUTION: Don't use this fn in runtime, only use in init-time.

  but used in runtime when a proper resource is settled."
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


;; * EDN


;; ** sources


(defprotocol IConfigEDNSource)


(defrecord ConfigEDNFileOrResourceSource [file-or-resource]
  IConfigEDNSource
  jio/IOFactory
  (make-reader [_ opts]
    ;; TODO: log trace level
    (println "FileOrResourceSource:" (str file-or-resource))
    (apply jio/reader file-or-resource opts)))


(defn config-edn-file-or-resource-source
  [^String path]
  (let [x (file-or-resource path)]
    (when x
      (->ConfigEDNFileOrResourceSource x))))


;; ** slurp


(defn slurp-edn-map
  "Read the file-or-resource specified by the path-segments, slurp it, and read it as edn."
  ([source slurp-opts]
   (slurp-edn-map source slurp-opts edn/read-string))
  ([source slurp-opts read-fn]
   (when (satisfies? IConfigEDNSource source)
     (let [ret (read-fn (apply slurp source (into [] cat slurp-opts)))]
       (if (map? ret)
         ret
         (throw
           (let [path (str source)]
             (ex-info (format "Expected edn map in: %s" path) {:path path}))))))))


;; ** integrant


(defn slurp-system-map
  "Read the file-or-resource specified by the path-segments, slurp it, and read it as edn."
  [source slurp-opts]
  (slurp-edn-map source slurp-opts ig/read-string))


;; * system


(defn merge-system-maps
  "The first map type return of slurp wins"
  [rules config sources]
  (env.merge/merge-maps
    rules
    (some
      (fn [src]
        (let [system-map (slurp-system-map src (:slurp-opts (meta src)))]
          (if (map? system-map)
            system-map
            nil)))
      sources)
    config))


(defn merge-system-maps-2
  "Generous merge-system-maps"
  [rules config sources]
  (env.merge/merge-maps
    rules
    (apply
      env.merge/merge-maps
      rules
      (map (fn [src] (env/slurp-system-map src (:slurp-opts (meta src)))) sources))
    config))


(def *system (atom {}))


;; * Shutdown


(defn- halt-system!
  []
  (when-let [sys @env/*system]
    (ig/halt! sys)))


(defn install-system-shutdown-hook!
  []
  (.addShutdownHook (Runtime/getRuntime) (Thread. halt-system!)))
