(ns loan-market.db.seed
  (:require [loan-market.domain.user :as user]))

(def seed-users
  [{:username "bank" :password "bankPass" :role "bank"}
   {:username "user" :password "userPass" :role "user"}
   {:username "admin" :password "adminPass" :role "admin"}])

(defn seed-if-empty!
  "Seed default users (bank/bankPass, user/userPass, admin/adminPass) if they are missing.
   Idempotent."
  [conn]
  (let [existing-count (user/count-users conn)]
    (when (or (zero? existing-count)
              (some (fn [{:keys [username]}]
                      (nil? (user/find-by-username conn username)))
                    seed-users))
      (println "[seed] Seeding missing users (bank, user, admin).")
      (doseq [s seed-users]
        (when-not (user/find-by-username conn (:username s))
          (user/create! conn (:username s) (:password s) (:role s))))))
  conn)
