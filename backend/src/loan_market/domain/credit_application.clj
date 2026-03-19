(ns loan-market.domain.credit-application
  (:require [clojure.string :as str]
            [datomic.client.api :as d]
            [loan-market.domain.user :as user]))

(def ^:private dob-re #"^\d{4}-\d{2}-\d{2}$")
(def ^:private allowed-marital-statuses
  #{"not married" "married" "divorced" "other"})

(defn- marital-status-from-user [u]
  (let [status (:user/marital-status u)]
    (when (some? status)
      (let [v (str/lower-case (str/trim (str status)))]
        (when-not (contains? allowed-marital-statuses v)
          (throw (ex-info "Invalid maritalStatus"
                          {:field "maritalStatus"
                           :allowed (sort allowed-marital-statuses)})))
        v))))

(defn- marital-status-from-application [m]
  (or (:credit-application/marital-status m)
      (when (contains? m :credit-application/married)
        (if (:credit-application/married m) "married" "not married"))))

(defn- body-val [m k]
  (or (get m k) (get m (name k))))

(defn- require-fields! [m ks]
  (doseq [k ks]
    (let [v (body-val m k)]
      (when (or (nil? v)
                (and (string? v) (str/blank? v)))
        (throw (ex-info "Missing required field"
                        {:field (name k)})))))
  m)

(defn- parse-long* [x]
  (cond
    (integer? x) (long x)
    (number? x) (long x)
    (string? x) (Long/parseLong (str/trim x))
    :else (throw (ex-info "Invalid integer field" {:value x}))))

(defn- parse-double* [x]
  (cond
    (number? x) (double x)
    (string? x) (Double/parseDouble (str/trim x))
    :else (throw (ex-info "Invalid numeric field" {:value x}))))

(defn create!
  "Create a credit application for a user email.
   Required payload keys: amount, income/yearlyIncome, debt.

   The remaining credit-application fields (name/email/dateOfBirth/maritalStatus/yearsWorking/industry)
   are copied from the authenticated user's Datomic profile."
  [conn email payload]
  (let [payload (cond-> payload
                   ;; Accept `income` as alias for `yearlyIncome`
                   (and (nil? (body-val payload :yearlyIncome))
                        (some? (body-val payload :income)))
                   (assoc :yearlyIncome (body-val payload :income)))]
    (require-fields! payload [:amount :yearlyIncome :debt])
    (let [u        (or (user/find-by-email conn email)
                       (throw (ex-info "User not found" {:email (str email)})))
          user-eid (or (user/eid-by-email conn email)
                        (throw (ex-info "User not found" {:email (str email)})))
          ;; Profile fields required for creating a credit application.
          dob       (:user/date-of-birth u)
          marital-status (marital-status-from-user u)
          yearsWork (:user/years-working u)
          industry  (:user/industry u)]
      (when (or (nil? dob) (and (string? dob) (str/blank? dob)))
        (throw (ex-info "Missing required field" {:field "dateOfBirth"})))
      (when-not (re-matches dob-re (str dob))
        (throw (ex-info "dateOfBirth must be YYYY-MM-DD" {:dateOfBirth (str dob)})))
      (when (nil? yearsWork)
        (throw (ex-info "Missing required field" {:field "yearsWorking"})))
      (when (or (nil? industry) (and (string? industry) (str/blank? industry)))
        (throw (ex-info "Missing required field" {:field "industry"})))
      (let [tx-base (cond-> {:credit-application/user          user-eid
                             :credit-application/name          (:user/name u)
                             :credit-application/email         (:user/email u)
                             :credit-application/amount        (parse-double* (body-val payload :amount))
                             :credit-application/yearlyIncome  (parse-double* (body-val payload :yearlyIncome))
                             :credit-application/debt          (parse-double* (body-val payload :debt))
                             :credit-application/date-of-birth (str dob)
                             :credit-application/years-working yearsWork
                             :credit-application/industry      (str industry)
                             :credit-application/created-at    (java.util.Date.)}
                      (some? marital-status)
                      (assoc :credit-application/marital-status marital-status))
            years-experience (body-val payload :yearsExperience)
            tx (cond-> tx-base
                 (some? years-experience)
                 (assoc :credit-application/years-experience (parse-long* years-experience)))
            tempid (str (java.util.UUID/randomUUID))
            res (d/transact conn {:tx-data [(assoc tx :db/id tempid)]})
            eid (get (:tempids res) tempid)]
        {:id eid}))))

(defn list-by-user
  "List applications for a user email with offset pagination.
   opts: {:page 1-based int, :pageSize int}. Returns {:items [] :page :pageSize :total}."
  [conn email {:keys [page pageSize]}]
  (let [p (max 1 (long (or page 1)))
        s (max 1 (min 100 (long (or pageSize 20))))
        offset (* (dec p) s)
        user-eid (or (user/eid-by-email conn email)
                     (throw (ex-info "User not found" {:email (str email)})))
        db (d/db conn)
        rows (d/q '[:find ?e ?createdAt
                    :in $ ?u
                    :where [?e :credit-application/user ?u]
                           [?e :credit-application/created-at ?createdAt]]
                  db
                  user-eid)
        sorted (->> rows
                    (sort-by second #(compare %2 %1))) ; created-at desc
        total (count sorted)
        page-eids (->> sorted
                       (drop offset)
                       (take s)
                       (map first))
        offer-rows (d/q '[:find ?appEid ?bankName ?bankEmail ?interest ?period
                          :in $ ?appEids
                          :where
                          [?offer :offer/credit-application ?appEid]
                          [?offer :offer/bank ?bankEid]
                          [?bankEid :user/name ?bankName]
                          [?bankEid :user/email ?bankEmail]
                          [?offer :offer/interest-rate ?interest]
                          [?offer :offer/repayment-period ?period]
                          [(contains? ?appEids ?appEid)]]
                        db (set page-eids))
        offers-by-app (reduce (fn [acc [appEid bankName bankEmail interest period]]
                                (update acc appEid (fnil conj []) {:bankName bankName
                                                                   :bankEmail bankEmail
                                                                   :interestRate interest
                                                                   :repaymentPeriod period}))
                              {} offer-rows)
        items (mapv (fn [eid]
                      (let [m (d/pull db
                                      '[:db/id
                                        :credit-application/name
                                        :credit-application/email
                                        :credit-application/amount
                                        :credit-application/yearlyIncome
                                        :credit-application/debt
                                        :credit-application/date-of-birth
                                        :credit-application/marital-status
                                        :credit-application/married
                                        :credit-application/years-working
                                        :credit-application/years-experience
                                        :credit-application/industry
                                        :credit-application/created-at]
                                      eid)
                            offers (get offers-by-app eid [])]
                        (cond-> {:id            (:db/id m)
                                 :name          (:credit-application/name m)
                                 :email         (:credit-application/email m)
                                 :amount        (:credit-application/amount m)
                                 :yearlyIncome  (:credit-application/yearlyIncome m)
                                 :debt          (:credit-application/debt m)
                                 :dateOfBirth   (:credit-application/date-of-birth m)
                                 :maritalStatus (marital-status-from-application m)
                                 :yearsWorking  (:credit-application/years-working m)
                                  :industry      (:credit-application/industry m)
                                 :offers        offers
                                 :createdAt     (some-> (:credit-application/created-at m) (.getTime))}
                                (some? (:credit-application/years-experience m))
                                (assoc :yearsExperience (:credit-application/years-experience m)))))
                    page-eids)]
    {:items items
     :page p
     :pageSize s
     :total total}))

(defn list-all
  "List all applications with offset pagination (bank use-case).
   opts: {:page 1-based int, :pageSize int}. Returns {:items [] :page :pageSize :total}.

   Admin listing: each item includes an `offers` array with offers from all banks."
  [conn {:keys [page pageSize]}]
  (let [p (max 1 (long (or page 1)))
        s (max 1 (min 100 (long (or pageSize 20))))
        offset (* (dec p) s)
        db (d/db conn)
        rows (d/q '[:find ?e ?createdAt
                    :where [?e :credit-application/created-at ?createdAt]]
                  db)
        sorted (->> rows (sort-by second #(compare %2 %1)))
        total (count sorted)
        page-eids (->> sorted (drop offset) (take s) (map first))
        offer-rows (d/q '[:find ?appEid ?bankName ?bankEmail ?interest ?period
                          :in $ ?appEids
                          :where
                          [?offer :offer/credit-application ?appEid]
                          [?offer :offer/bank ?bankEid]
                          [?bankEid :user/name ?bankName]
                          [?bankEid :user/email ?bankEmail]
                          [?offer :offer/interest-rate ?interest]
                          [?offer :offer/repayment-period ?period]
                          [(contains? ?appEids ?appEid)]]
                        db (set page-eids))
        offers-by-app (reduce (fn [acc [appEid bankName bankEmail interest period]]
                                 (update acc appEid (fnil conj []) {:bankName bankName
                                                                      :bankEmail bankEmail
                                                                      :interestRate interest
                                                                      :repaymentPeriod period}))
                               {} offer-rows)
        items (mapv (fn [eid]
                       (let [m (d/pull db
                                       '[:db/id
                                         :credit-application/name
                                         :credit-application/email
                                         :credit-application/amount
                                         :credit-application/yearlyIncome
                                         :credit-application/debt
                                         :credit-application/date-of-birth
                                         :credit-application/marital-status
                                         :credit-application/married
                                         :credit-application/years-working
                                         :credit-application/years-experience
                                         :credit-application/industry
                                         :credit-application/created-at]
                                       eid)
                             offers (get offers-by-app eid [])]
                         (cond-> {:id            (:db/id m)
                                  :name          (:credit-application/name m)
                                  :email         (:credit-application/email m)
                                  :amount        (:credit-application/amount m)
                                  :yearlyIncome  (:credit-application/yearlyIncome m)
                                  :debt          (:credit-application/debt m)
                                  :dateOfBirth   (:credit-application/date-of-birth m)
                                  :maritalStatus (marital-status-from-application m)
                                  :yearsWorking  (:credit-application/years-working m)
                                  :industry      (:credit-application/industry m)
                                  :offers        offers
                                  :createdAt     (some-> (:credit-application/created-at m) (.getTime))}
                           (some? (:credit-application/years-experience m))
                           (assoc :yearsExperience (:credit-application/years-experience m)))))
                     page-eids)]
    {:items items
     :page p
     :pageSize s
     :total total}))

(defn list-by-bank
  "Bank listing: same app shape as admin listing, but includes only the calling bank's offer
   as `interestRate` and `repaymentPeriod` (and omits the `offers` array)."
  [conn bank-email {:keys [page pageSize]}]
  (let [p (max 1 (long (or page 1)))
        s (max 1 (min 100 (long (or pageSize 20))))
        offset (* (dec p) s)
        db (d/db conn)
        bank-eid (or (user/eid-by-email conn bank-email)
                      (throw (ex-info "Bank user not found" {:email (str bank-email)})))
        rows (d/q '[:find ?e ?createdAt
                    :where [?e :credit-application/created-at ?createdAt]]
                  db)
        sorted (->> rows (sort-by second #(compare %2 %1)))
        total (count sorted)
        page-eids (->> sorted (drop offset) (take s) (map first))
        offer-rows (d/q '[:find ?appEid ?interest ?period
                          :in $ ?bankEid ?appEids
                          :where
                          [?offer :offer/bank ?bankEid]
                          [?offer :offer/credit-application ?appEid]
                          [?offer :offer/interest-rate ?interest]
                          [?offer :offer/repayment-period ?period]
                          [(contains? ?appEids ?appEid)]]
                        db bank-eid (set page-eids))
        offers-by-app (into {} (map (fn [[appEid interest period]]
                                        [appEid {:interestRate interest
                                                 :repaymentPeriod period}]))
                             offer-rows)
        items (mapv (fn [eid]
                       (let [m (d/pull db
                                       '[:db/id
                                         :credit-application/name
                                         :credit-application/email
                                         :credit-application/amount
                                         :credit-application/yearlyIncome
                                         :credit-application/debt
                                         :credit-application/date-of-birth
                                         :credit-application/marital-status
                                         :credit-application/married
                                         :credit-application/years-working
                                         :credit-application/years-experience
                                         :credit-application/industry
                                         :credit-application/created-at]
                                       eid)
                             offer (get offers-by-app eid)]
                         (cond-> {:id            (:db/id m)
                                  :name          (:credit-application/name m)
                                  :email         (:credit-application/email m)
                                  :amount        (:credit-application/amount m)
                                  :yearlyIncome  (:credit-application/yearlyIncome m)
                                  :debt          (:credit-application/debt m)
                                  :dateOfBirth   (:credit-application/date-of-birth m)
                                  :maritalStatus (marital-status-from-application m)
                                  :yearsWorking  (:credit-application/years-working m)
                                  :industry      (:credit-application/industry m)
                                  :createdAt     (some-> (:credit-application/created-at m) (.getTime))}
                           (some? offer)
                           (assoc :interestRate (:interestRate offer)
                                  :repaymentPeriod (:repaymentPeriod offer))
                           (some? (:credit-application/years-experience m))
                           (assoc :yearsExperience (:credit-application/years-experience m)))))
                     page-eids)]
    {:items items
     :page p
     :pageSize s
     :total total}))

(defn offer!
  "Bank submits an offer for a credit application.
   Payload expects:
   - interestRate (double)
   - repaymentPeriod (long)"
  [conn bank-email application-id {:keys [interestRate repaymentPeriod] :as payload}]
  (let [app-eid (Long/parseLong (str application-id))
        db      (d/db conn)
        _       (when-not (d/pull db '[:db/id] app-eid)
                  (throw (ex-info "Credit application not found" {:id application-id})))
        bank-eid (or (user/eid-by-email conn bank-email)
                      (throw (ex-info "Bank user not found" {:email (str bank-email)})))
        ir      (or interestRate (:interestRate payload))
        rp      (or repaymentPeriod (:repaymentPeriod payload))]
    (when (or (nil? ir) (nil? rp))
      (throw (ex-info "interestRate and repaymentPeriod are required"
                      {:field (if (nil? ir) "interestRate" "repaymentPeriod")})))
    (let [interest (parse-double* ir)
          period   (parse-long* rp)
          offer-key (str bank-eid ":" app-eid)
          existing-offer-eid (ffirst (d/q '[:find ?e :in $ ?k :where [?e :offer/key ?k]] db offer-key))
          tx (if existing-offer-eid
               {:db/id existing-offer-eid
                :offer/interest-rate interest
                :offer/repayment-period period
                :offer/bank bank-eid
                :offer/credit-application app-eid}
               {:offer/key offer-key
                :offer/bank bank-eid
                :offer/credit-application app-eid
                :offer/interest-rate interest
                :offer/repayment-period period})]
      (d/transact conn {:tx-data [tx]})
      conn)))

(defn delete!
  "Delete a credit application by its numeric Datomic entity id (the `:id` returned from create!/list*)."
  [conn application-id]
  (let [db  (d/db conn)
        app-eid (Long/parseLong (str application-id))
        _   (when-not (d/pull db '[:db/id] app-eid)
              (throw (ex-info "Credit application not found" {:id application-id})))
        offer-eids (map first
                         (d/q '[:find ?offerEid :in $ ?appEid
                                :where
                                [?offerEid :offer/credit-application ?appEid]]
                              db
                              app-eid))
        tx-data (concat (mapv (fn [oid] [:db/retractEntity oid]) offer-eids)
                        [[:db/retractEntity app-eid]])]
    (d/transact conn {:tx-data tx-data})
    conn))

