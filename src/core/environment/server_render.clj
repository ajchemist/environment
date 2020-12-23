(ns environment.server-render
  (:require
   [clojure.java.io :as jio]
   [integrant.core :as ig]
   [environment.hawk :as env.hawk]
   [environment.integrant :as env.ig]
   ))


(set! *warn-on-reflection* true)


(defn- read-file
  ([f]
   (read-file f {}))
  ([f opts]
   (try
     (read-string (apply slurp f (into [] cat opts)))
     (catch Exception _ {}))))


(defn update-config
  [old new]
  (if (seq new) new old))


(defmethod ig/init-key ::reference
  [_ source]
  (atom (read-file source)))


(defmethod ig/halt-key! ::reference
  [_ _])


(defmethod ig/init-key ::file-source-watcher-args
  [_ {:keys [reference source callback]
      :or   {callback (fn [_ _ _])}}]
  (let [source (jio/file source)]
    [
     [{:paths  [(.getParent source)]
       :filter (env.hawk/file-changed? source)
       :handler
       (fn [ctx {:keys [file] :as event}]
         (swap! reference update-config (read-file source))
         (println "server-render file-source changed.")
         (callback reference ctx event))}]]))


(defmethod ig/halt-key! ::file-source-watcher-args [_ _])


(derive ::source ::env.ig/value)
(derive ::watcher ::env.hawk/watcher)


(defn system-map
  "`watch-dir` for configuration

  [::file-source-watcher-args :file]은 configuration 권장하진 않지만, system-map
  구성 후 assoc programmatic interface로 조작가능"
  [{:keys [watch-dir]}]
  {::source                   (jio/file watch-dir "server-render.edn")
   ::reference                (ig/ref ::source)
   ::file-source-watcher-args {:reference (ig/ref ::reference)
                               :source    (ig/ref ::source)}
   ::watcher                  (ig/ref ::file-source-watcher-args)})


(set! *warn-on-reflection* false)
