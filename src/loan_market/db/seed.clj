(ns loan-market.db.seed
  (:require [loan-market.domain.user :as user]))

(def seed-users
  [{:username "bank" :password "bankPass" :role "bank"}
   {:username "user" :password "userPass" :role "user"}])

(defn seed-if-empty!
  "If no users exist, transact seed users (bank/bankPass, user/userPass). Idempotent."
  [conn]
  (when (zero? (user/count-users conn))
    (doseq [s seed-users]
      (user/create! conn (:username s) (:password s) (:role s))))
  conn)
