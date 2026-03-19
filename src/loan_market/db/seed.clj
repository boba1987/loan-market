(ns loan-market.db.seed
  (:require [loan-market.domain.credit-application :as credit-application]
            [loan-market.domain.user :as user]))

(def seed-users
  [{:username "bank" :password "bankPass" :role "bank"
    :name "OTP Bank" :email "user@otp.com"}
   {:username "user" :password "userPass" :role "user"
    :name "Jane Doe" :email "jane@example.com"}
   {:username "admin" :password "adminPass" :role "admin"
    :name "Admin" :email "admin@example.com"}])

(defn seed-if-empty!
  "Seed default users (bank/bankPass, user/userPass, admin/adminPass) if they are missing.
   Idempotent."
  [conn]
  (let [existing-count (user/count-users conn)
        missing-any?
        (or (zero? existing-count)
            (some (fn [{:keys [username]}]
                    (nil? (user/find-by-username conn username)))
                  seed-users))]
    (when missing-any?
      (println "[seed] Seeding missing users (bank, user, admin).")
      (doseq [s seed-users]
        (when-not (user/find-by-username conn (:username s))
          (user/create! conn (:username s) (:password s) (:role s)
                         {:name (:name s) :email (:email s)}))))

    ;; Ensure seeded profile fields exist even for users created in older DB versions.
    (doseq [s seed-users]
      (let [u (user/find-by-username conn (:username s))]
        (when (or (nil? (:user/name u)) (nil? (:user/email u)))
          (user/update! conn (:username s)
                         {:name (:name s) :email (:email s)}))))

    ;; Seed a sample credit application for the default `user` if none exists yet.
    (let [r (credit-application/list-by-user conn "user" {:page 1 :pageSize 1})]
      (when (zero? (:total r))
        (credit-application/create!
          conn
          "user"
          {:name "Jane Doe"
           :email "jane@example.com"
           :income 50000
           :debt 2000
           :dateOfBirth "1990-01-30"
           :married true
           :yearsWorking 7
           :amount 10000
           :industry "Software"})))
    conn))
