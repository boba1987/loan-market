(ns loan-market.db.seed
  (:require [loan-market.domain.credit-application :as credit-application]
            [loan-market.domain.user :as user]))

(def seed-users
  [{:email "otp@bank.com" :password "bankPass" :role "bank"
    :name "OTP Bank"}
   {:email "jane@user.com" :password "userPass" :role "user"
    :name "Jane Doe"
    :dateOfBirth "1990-01-30"
    :married true
    :yearsWorking 7
    :industry "Software"}
   {:email "admin@admin.com" :password "adminPass" :role "admin"
    :name "Admin"}])

(defn seed-if-empty!
  "Seed default users (otp@bank.com, jane@user.com, admin@admin.com) if they are missing.
   Idempotent."
  [conn]
  (let [existing-count (user/count-users conn)
        missing-any?
        (or (zero? existing-count)
            (some (fn [{:keys [email]}]
                    (nil? (user/find-by-email conn email)))
                  seed-users))]
    (when missing-any?
      (println "[seed] Seeding missing users (bank, user, admin).")
      (doseq [s seed-users]
        (when-not (user/find-by-email conn (:email s))
          (user/create! conn (:email s) (:password s) (:role s)
                         {:name (:name s)
                          :dateOfBirth (:dateOfBirth s)
                          :married (:married s)
                          :yearsWorking (:yearsWorking s)
                          :industry (:industry s)}))))

    ;; Ensure seeded profile fields exist even for users created in older DB versions.
    (doseq [s seed-users]
      (let [u (user/find-by-email conn (:email s))
            needs?
            (or (nil? (:user/name u))
                (nil? (:user/email u))
                (and (some? (:dateOfBirth s)) (nil? (:user/date-of-birth u)))
                (and (some? (:married s)) (nil? (:user/married u)))
                (and (some? (:yearsWorking s)) (nil? (:user/years-working u)))
                (and (some? (:industry s)) (nil? (:user/industry u))))]
        (when needs?
          (user/update! conn (:email s)
                         {:name (:name s)
                          :email (:email s)
                          :dateOfBirth (:dateOfBirth s)
                          :married (:married s)
                          :yearsWorking (:yearsWorking s)
                          :industry (:industry s)}))))

    ;; Seed a sample credit application for the default `user` if none exists yet.
    (let [r (credit-application/list-by-user conn "jane@user.com" {:page 1 :pageSize 1})]
      (when (zero? (:total r))
        (credit-application/create!
          conn
          "jane@user.com"
          {:name "Jane Doe"
           :email "jane@user.com"
           :income 50000
           :debt 2000
           :dateOfBirth "1990-01-30"
           :married true
           :yearsWorking 7
           :amount 10000
           :industry "Software"})))
    conn))
