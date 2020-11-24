(ns environment.hawk
  (:require
   [integrant.core :as ig]
   [hawk.core :as hawk]
   )
  (:import
   java.io.File
   ))


;; * hawk filter fns


(defn file-changed?
  [^File file]
  (let [file-source (.getCanonicalFile file)]
    (fn [_ctx {:keys [file kind]}]
      (and (= file file-source) (#{:create :modify} kind)))))


;; * integrant


(defmethod ig/init-key ::watcher
  [_ args]
  (apply hawk/watch! args))


(defmethod ig/halt-key! ::watcher
  [_ watcher]
  (hawk/stop! watcher))
