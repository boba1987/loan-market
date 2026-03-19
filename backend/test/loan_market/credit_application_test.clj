(ns loan-market.credit-application-test
  (:require [midje.sweet :refer [facts fact]
                           :refer-macros [=>]]
            [loan-market.config :as config]
            [loan-market.db.core :as db]
            [loan-market.db.seed :as seed]
            [loan-market.domain.credit-application :as credit-application]
            [loan-market.domain.user :as user]))

(facts "credit applications"
  (with-redefs [config/storage-dir (constantly nil)]
    (let [conn (db/connect)]
      (seed/seed-if-empty! conn)

      (fact "create! stores an application for user"
        (let [created (credit-application/create!
                       conn
                       "jane@user.com"
                       {:name "Jane Doe"
                        :email "jane@user.com"
                        :amount 10000
                        :yearlyIncome 50000
                        :debt 2000
                        :dateOfBirth "1990-01-30"
                        :married true
                        :yearsWorking 7
                        :yearsExperience 5
                        :industry "Software"})]
          (:id created) => some?))

      (fact "list-by-user returns items and supports pagination"
        (let [r1 (credit-application/list-by-user conn "jane@user.com" {:page 1 :pageSize 1})
              r2 (credit-application/list-by-user conn "jane@user.com" {:page 2 :pageSize 1})]
          (:page r1) => 1
          (:pageSize r1) => 1
          (:total r1) => pos?
          (count (:items r1)) => 1
        (<= (count (:items r2)) 1) => true))

      (fact "list-all returns all applications (bank use-case)"
        (let [all (credit-application/list-all conn {:page 1 :pageSize 50})]
          (:total all) => pos?
          (<= (count (:items all)) 50) => true))

      (fact "banks can submit offers per credit application (one per bank)"
        (let [created (credit-application/create!
                         conn
                         "jane@user.com"
                         {:name "Jane Doe"
                          :email "jane@user.com"
                          :amount 10000
                          :yearlyIncome 50000
                          :debt 2000
                          :dateOfBirth "1990-01-30"
                          :married true
                          :yearsWorking 7
                          :yearsExperience 5
                          :industry "Software"})
              _ (user/create! conn "bank2@bank.com" "bank2Pass" "bank" {:name "Bank2"})
              _offered1 (credit-application/offer!
                          conn
                          "otp@bank.com"
                          (:id created)
                          {:interestRate 4.25
                           :repaymentPeriod 60})
              _offered2 (credit-application/offer!
                          conn
                          "bank2@bank.com"
                          (:id created)
                          {:interestRate 5.0
                           :repaymentPeriod 72})
              _offered3 (credit-application/offer!
                          conn
                          "otp@bank.com"
                          (:id created)
                          {:interestRate 4.75
                           :repaymentPeriod 65})
              admin (credit-application/list-all conn {:page 1 :pageSize 50})
              item (first (filter #(= (:id %) (:id created)) (:items admin)))
              offers (:offers item)
              bank-offer (first (filter #(= (:bankEmail %) "otp@bank.com") offers))
              bank2-offer (first (filter #(= (:bankEmail %) "bank2@bank.com") offers))
              bank-view (credit-application/list-by-bank conn "otp@bank.com" {:page 1 :pageSize 50})
              bank-item (first (filter #(= (:id %) (:id created)) (:items bank-view)))]
          (count offers) => 2
          (:interestRate bank-offer) => 4.75
          (:repaymentPeriod bank-offer) => 65
          (:interestRate bank2-offer) => 5.0
          (:repaymentPeriod bank2-offer) => 72
          (:interestRate bank-item) => 4.75
          (:repaymentPeriod bank-item) => 65)))))

