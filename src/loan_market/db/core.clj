(ns loan-market.db.core
  (:require [datomic.client.api :as d]
            [loan-market.config :as config]))

(defn client
  "Create a Datomic Local client. Defaults to :storage-dir :mem (in-memory); set config/storage-dir for persistence."
  []
  (d/client (merge {:server-type :datomic-local
                    :system      (config/datomic-system)
                    :storage-dir :mem}
                   (when-let [sd (config/storage-dir)]
                     {:storage-dir sd}))))

(defn ensure-database!
  "Create database if it does not exist. Returns client."
  [c]
  (let [db-name (config/db-name)]
    (when-not (d/create-database c {:db-name db-name})
      nil) ; already exists
    c))

(def user-schema
  [{:db/ident       :user/username
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}
   {:db/ident       :user/password-hash
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :user/role
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])

(defn schema-exists? [conn]
  (let [db (d/db conn)
        result (d/q '[:find ?e :where [?e :db/ident :user/username]] db)]
    (boolean (seq result))))

(defn ensure-schema!
  "Transact user schema if not already present."
  [conn]
  (when-not (schema-exists? conn)
    (d/transact conn {:tx-data user-schema}))
  conn)

(defn connect
  "Create client, ensure database exists, connect, ensure schema. Returns connection."
  []
  (let [c (client)]
    (ensure-database! c)
    (let [conn (d/connect c {:db-name (config/db-name)})]
      (ensure-schema! conn)
      conn)))
