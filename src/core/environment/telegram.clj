(ns environment.telegram
  {:references ["https://github.com/tonsky/grumpy/blob/master/src/grumpy/telegram.clj"]}
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [clj-http.client :as http]
   [user.ring.alpha :as user.ring]
   ))


(s/def :telegram/token string?)
(s/def :telegram/method string?)


(defn format-request-url
  [token method]
  (str "https://api.telegram.org/bot" token method))


(defn- request-url-transformer
  [{:keys [:telegram/token :telegram/method] :as req}]
  (update req :url #(or % (format-request-url token method))))


(def ^{:arglists '([request] [request response raise])}
  request
  (-> http/request
    (user.ring/wrap-transform-request request-url-transformer)
    (user.ring/wrap-transform-request
      (fn
        [request]
        (merge
          {:request-method     :post
           :as                 :json-string-keys
           :content-type       :json
           :connection-timeout 8899}
          request)))
    (user.ring/wrap-meta-response)))


(defn client
  [req]
  (try
    (request req)
    (catch Exception e
      (let [{:keys [url]} (request-url-transformer req)]
        (cond
          (re-find #"Bad Request: message is not modified" (:body (ex-data e)))
          (println "Telegram request failed:" url (pr-str req))

          :else
          (do
            (println "Telegram request failed:" url (pr-str req))
            (throw e)))))))


;; * sugar


(defn set-webhook
  [token webhook-url]
  (client
    {:telegram/token  token
     :telegram/method "/setWebhook"
     :query-params    {:url webhook-url}}))


(defn delete-webhook
  [token]
  (client
    {:telegram/token  token
     :telegram/method "/deleteWebhook"}))
