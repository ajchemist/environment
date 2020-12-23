(ns environment.telegram.log
  (:require
   [clojure.spec.alpha :as s]
   [environment.telegram :as tg]
   ))


(s/def ::publisher-config (s/keys :req [:telegram/token :telegram/method]))
(s/def ::publisher-configs (s/map-of ident? ::publisher-config))


(defonce *agent (agent {}))


(defonce *publishers (atom {}))


(defn log
  [data]
  (run!
    (fn
      [[id {:keys [:telegram/token :telegram/method] :as cfg}]]
      (send *agent
        (fn [state]
          (assoc state
            id
            (tg/client
              {:telegram/token  token
               :telegram/method method
               :form-params     (merge (:params cfg) data)})))))
    @*publishers))


(defn start-pulisher!
  [configs]
  (transduce
    (map
      (fn [{:keys [id] :as cfg}]
        (let [id (keyword (or id (gensym)))]
          [id (dissoc cfg :id)])))
    (fn
      ([ret] (reset! *publishers ret))
      ([ret [id cfg]] (assoc ret id cfg)))
    @*publishers
    configs))
