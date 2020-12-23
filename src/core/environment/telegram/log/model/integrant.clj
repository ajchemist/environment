(ns environment.telegram.log.model.integrant
  (:require
   [integrant.core :as ig]
   [environment.telegram.log :as tg.log]
   ))


(defmethod ig/pre-init-spec ::publisher [_] ::tg.log/publisher-configs)


(defmethod ig/init-key ::publisher
  [ident configs]
  (println "Start telegram log" ident)
  (tg.log/start-pulisher! configs))


(defmethod ig/halt-key! ::publisher
  [_ _])
