(ns loan-market.db.core
  (:require [datomic.client.api :as d]
            [loan-market.config :as config]))

(defn client
  []
  (d/client (merge {:server-type :datomic-local
                    :system      (config/datomic-system)
                    :storage-dir :mem}
                   (when-let [sd (config/storage-dir)]
                     {:storage-dir sd}))))

(defn ensure-database!
  [c]
  (let [db-name (config/db-name)]
    (when-not (d/create-database c {:db-name db-name})
      nil)
    c))

(def user-schema
  [{:db/ident       :user/name
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :user/email
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}
   {:db/ident       :user/password-hash
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :user/role
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])

(def credit-application-schema
  [{:db/ident       :credit-application/user
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident       :credit-application/name
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :credit-application/email
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :credit-application/amount
    :db/valueType   :db.type/double
    :db/cardinality :db.cardinality/one}
   {:db/ident       :credit-application/yearlyIncome
    :db/valueType   :db.type/double
    :db/cardinality :db.cardinality/one}
   {:db/ident       :credit-application/debt
    :db/valueType   :db.type/double
    :db/cardinality :db.cardinality/one}
   {:db/ident       :credit-application/date-of-birth
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :credit-application/married
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}
   {:db/ident       :credit-application/years-working
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}
   {:db/ident       :credit-application/years-experience
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}
   {:db/ident       :credit-application/industry
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :credit-application/created-at
    :db/valueType   :db.type/instant
    :db/cardinality :db.cardinality/one}])

(def offer-schema
  [{:db/ident         :offer/bank
    :db/valueType     :db.type/ref
    :db/cardinality   :db.cardinality/one}
   {:db/ident         :offer/credit-application
    :db/valueType     :db.type/ref
    :db/cardinality   :db.cardinality/one}
   {:db/ident         :offer/interest-rate
    :db/valueType     :db.type/double
    :db/cardinality   :db.cardinality/one}
   {:db/ident         :offer/repayment-period
    :db/valueType     :db.type/long
    :db/cardinality   :db.cardinality/one}
   ;; Unique deterministic key to enforce one offer per (bank, application).
   {:db/ident         :offer/key
    :db/valueType     :db.type/string
    :db/unique        :db.unique/identity
    :db/cardinality   :db.cardinality/one}])

(defn- ident-exists?
  [db ident]
  (boolean (seq (d/q '[:find ?e :in $ ?ident :where [?e :db/ident ?ident]] db ident))))

(defn- missing-schema
  "Return schema attribute maps whose :db/ident is not present in db."
  [db schema]
  (->> schema
       (remove (fn [attr] (ident-exists? db (:db/ident attr))))
       (vec)))

(defn ensure-schema!
  [conn]
  (let [db (d/db conn)
        missing (into []
                      cat
                      [(missing-schema db user-schema)
                       (missing-schema db credit-application-schema)
                       (missing-schema db offer-schema)])]
    (when (seq missing)
      (d/transact conn {:tx-data missing})))
  conn)

(defn connect
  "Create client, ensure database exists, connect, ensure schema. Returns connection."
  []
  (let [c (client)]
    (ensure-database! c)
    (let [conn (d/connect c {:db-name (config/db-name)})]
      (ensure-schema! conn)
      conn)))
