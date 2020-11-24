(ns environment.integrant
  (:require
   [integrant.core :as ig]
   ))


(defmethod ig/init-key ::value [_ o] o)
(defmethod ig/halt-key! ::value [_ _])
