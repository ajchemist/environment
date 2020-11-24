(ns environment.server-render
  (:require
   [clojure.java.io :as jio]
   [integrant.core :as ig]
   [environment.hawk :as env.hawk]
   [environment.integrant :as env.ig]
   ))


(defmethod ig/init-key ::file-source-watcher-args
  [_ {:keys [reference watch-dir file callback]
      :or   {file     "server-render.edn"
             callback (fn [_ _ _])}}]
  [
   [{:paths  [(str watch-dir)]
     :filter (env.hawk/file-changed? (jio/file watch-dir file))
     :handler
     (fn [ctx {:keys [file] :as event}]
       (reset! reference (read-string (slurp file)))
       (println "server-render file-source changed.")
       (callback reference ctx event))}]])


(defmethod ig/halt-key! ::file-source-watcher-args [_ _])


(derive ::reference ::env.ig/value)
(derive ::watcher ::env.hawk/watcher)


(defn system-map
  "`watch-dir` for configuration

  [::file-source-watcher-args :file]은 configuration 권장하진 않지만, system-map
  구성 후 assoc programmatic interface로 조작가능"
  [{:keys [watch-dir]}]
  {::reference                (atom {})
   ::file-source-watcher-args {:reference (ig/ref ::reference)
                               :watch-dir watch-dir}
   ::watcher                  (ig/ref ::file-source-watcher-args)})
