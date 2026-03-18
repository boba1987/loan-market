(ns loan-market.domain.credit-application
  (:require [clojure.string :as str]
            [datomic.client.api :as d]
            [loan-market.domain.user :as user]))

(def ^:private dob-re #"^\d{4}-\d{2}-\d{2}$")

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
  "Create a credit application for username. Payload keys can be keywords or strings.
   Expects dateOfBirth as ISO YYYY-MM-DD string."
  [conn username payload]
  (require-fields! payload [:name
                            :email
                            :amount
                            :yearlyIncome
                            :debt
                            :dateOfBirth
                            :married
                            :yearsWorking
                            :yearsExperience
                            :industry])
  (let [user-eid (or (user/eid-by-username conn username)
                     (throw (ex-info "User not found" {:username (str username)})))
        dob (str (body-val payload :dateOfBirth))]
    (when-not (re-matches dob-re dob)
      (throw (ex-info "dateOfBirth must be YYYY-MM-DD" {:dateOfBirth dob})))
    (let [tx {:credit-application/user            user-eid
              :credit-application/name            (str (body-val payload :name))
              :credit-application/email           (str (body-val payload :email))
              :credit-application/amount          (parse-double* (body-val payload :amount))
              :credit-application/yearlyIncome   (parse-double* (body-val payload :yearlyIncome))
              :credit-application/debt            (parse-double* (body-val payload :debt))
              :credit-application/date-of-birth   dob
              :credit-application/married         (boolean (body-val payload :married))
              :credit-application/years-working   (parse-long* (body-val payload :yearsWorking))
              :credit-application/years-experience (parse-long* (body-val payload :yearsExperience))
              :credit-application/industry        (str (body-val payload :industry))
              :credit-application/created-at      (java.util.Date.)}
          tempid (str (java.util.UUID/randomUUID))
          res (d/transact conn {:tx-data [(assoc tx :db/id tempid)]})
          eid (get (:tempids res) tempid)]
      {:id eid})))

(defn list-by-user
  "List applications for username with offset pagination.
   opts: {:page 1-based int, :pageSize int}. Returns {:items [] :page :pageSize :total}."
  [conn username {:keys [page pageSize]}]
  (let [p (max 1 (long (or page 1)))
        s (max 1 (min 100 (long (or pageSize 20))))
        offset (* (dec p) s)
        user-eid (or (user/eid-by-username conn username)
                     (throw (ex-info "User not found" {:username (str username)})))
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
        items (mapv (fn [eid]
                      (let [m (d/pull db
                                      '[:db/id
                                        :credit-application/name
                                        :credit-application/email
                                        :credit-application/amount
                                        :credit-application/yearlyIncome
                                        :credit-application/debt
                                        :credit-application/date-of-birth
                                        :credit-application/married
                                        :credit-application/years-working
                                        :credit-application/years-experience
                                        :credit-application/industry
                                        :credit-application/created-at]
                                      eid)]
                        {:id              (:db/id m)
                         :name            (:credit-application/name m)
                         :email           (:credit-application/email m)
                         :amount          (:credit-application/amount m)
                         :yearlyIncome   (:credit-application/yearlyIncome m)
                         :debt            (:credit-application/debt m)
                         :dateOfBirth     (:credit-application/date-of-birth m)
                         :married         (:credit-application/married m)
                         :yearsWorking    (:credit-application/years-working m)
                         :yearsExperience (:credit-application/years-experience m)
                         :industry        (:credit-application/industry m)
                         :createdAt       (some-> (:credit-application/created-at m) (.getTime))}))
                    page-eids)]
    {:items items
     :page p
     :pageSize s
     :total total}))

(defn list-all
  "List all applications with offset pagination (bank use-case).
   opts: {:page 1-based int, :pageSize int}. Returns {:items [] :page :pageSize :total}."
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
        items (mapv (fn [eid]
                      (let [m (d/pull db
                                      '[:db/id
                                        :credit-application/name
                                        :credit-application/email
                                        :credit-application/amount
                                        :credit-application/yearlyIncome
                                        :credit-application/debt
                                        :credit-application/date-of-birth
                                        :credit-application/married
                                        :credit-application/years-working
                                        :credit-application/years-experience
                                        :credit-application/industry
                                        :credit-application/created-at]
                                      eid)]
                        {:id              (:db/id m)
                         :name            (:credit-application/name m)
                         :email           (:credit-application/email m)
                         :amount          (:credit-application/amount m)
                         :yearlyIncome   (:credit-application/yearlyIncome m)
                         :debt            (:credit-application/debt m)
                         :dateOfBirth     (:credit-application/date-of-birth m)
                         :married         (:credit-application/married m)
                         :yearsWorking    (:credit-application/years-working m)
                         :yearsExperience (:credit-application/years-experience m)
                         :industry        (:credit-application/industry m)
                         :createdAt       (some-> (:credit-application/created-at m) (.getTime))}))
                    page-eids)]
    {:items items
     :page p
     :pageSize s
     :total total}))

(defn delete!
  "Delete a credit application by its numeric Datomic entity id (the `:id` returned from create!/list*)."
  [conn application-id]
  (let [db  (d/db conn)
        eid (Long/parseLong (str application-id))]
    (when-not (d/pull db '[:db/id] eid)
      (throw (ex-info "Credit application not found" {:id application-id})))
    ;; Use retractEntity so deletion doesn't depend on referenced entities
    ;; (e.g. :credit-application/user may point to a user that no longer exists).
    (d/transact conn {:tx-data [[:db/retractEntity eid]]})
    conn))

