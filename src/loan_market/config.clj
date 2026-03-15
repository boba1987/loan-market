(ns loan-market.config
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(def default-port 3000)
(def default-db-name "loan-market")
(def default-datomic-system "dev")

(defn- parse-port
  [s]
  (when (and (string? s) (not (str/blank? s)))
    (try
      (Long/parseLong (str/trim s))
      (catch NumberFormatException _ nil))))

(defn- get-env [k default]
  (let [v (System/getenv (name k))]
    (if (and (string? v) (not (str/blank? v)))
      (str/trim v)
      default)))

(defn- env-file-value [key-pattern]
  (let [env-file (io/file ".env")]
    (when (.exists env-file)
      (when-let [content (slurp env-file)]
        (some (fn [line]
                (when-let [m (re-find key-pattern (str/trim line))]
                  (nth m 1)))
              (str/split-lines content))))))

(defn- port-from-env-file
  "Read PORT from .env in project root (current working directory)."
  []
  (when-let [s (env-file-value #"PORT\s*=\s*(\S+)")]
    (parse-port s)))

(defn port
  "Port to bind: PORT env var, then .env file, else default 3000."
  []
  (or (parse-port (System/getenv "PORT"))
      (port-from-env-file)
      default-port))

(defn jwt-secret
  "JWT signing secret: JWT_SECRET env var or .env, required for auth."
  []
  (or (get-env :JWT_SECRET nil)
      (some-> (env-file-value #"JWT_SECRET\s*=\s*(.+)") str/trim)
      (throw (ex-info "JWT_SECRET is required (set in env or .env)" {}))))

(defn db-name
  []
  (or (get-env :DATOMIC_DB_NAME nil)
      (env-file-value #"DATOMIC_DB_NAME\s*=\s*(\S+)")
      default-db-name))

(defn datomic-system
  []
  (or (get-env :DATOMIC_SYSTEM nil)
      (env-file-value #"DATOMIC_SYSTEM\s*=\s*(\S+)")
      default-datomic-system))

(defn storage-dir
  "Datomic Local storage: absolute path string, or :mem for in-memory (default). Set DATOMIC_STORAGE_DIR or ~/.datomic/local.edn for persistence."
  []
  (when-let [v (or (get-env :DATOMIC_STORAGE_DIR nil)
                   (env-file-value #"DATOMIC_STORAGE_DIR\s*=\s*(\S+)"))]
    (str/trim v)))
