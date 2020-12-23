(ns environment.telegram-test
  (:require
   [clojure.test :as test :refer [deftest is are testing]]
   [clojure.string :as str]
   [environment.telegram :as tg]
   ))


(def tg-token (System/getenv "TG_TOKEN"))
(def tg-chat-id (System/getenv "TG_CHAT_ID"))
(def gh-sha (System/getenv "GITHUB_SHA"))


(assert (not (str/blank? tg-token)))
(assert (not (str/blank? tg-chat-id)))


(deftest main
  (prn
    (update
      (tg/client
        {:telegram/method "/getMe"
         :telegram/token  tg-token})
      "result" dissoc "id"))
  (is
    (get
      (tg/client
        {:telegram/method "/getUpdates"
         :telegram/token  tg-token})
      "ok"))
  (when-not (str/blank? gh-sha)
    (tg/client
      {:telegram/method "/sendMessage"
       :telegram/token  tg-token
       :form-params     {:chat_id tg-chat-id :text (str "Message from test-runner <" (subs gh-sha 0 8) ">.")}
       })
    ))
