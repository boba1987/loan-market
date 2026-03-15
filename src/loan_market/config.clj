(ns loan-market.config
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(def default-port 3000)

(defn- parse-port
  [s]
  (when (and (string? s) (not (str/blank? s)))
    (try
      (Long/parseLong (str/trim s))
      (catch NumberFormatException _ nil))))

(defn- port-from-env-file
  "Read PORT from .env in project root (current working directory)."
  []
  (let [env-file (io/file ".env")]
    (when (.exists env-file)
      (when-let [content (slurp env-file)]
        (some (fn [line]
                (when-let [m (re-find #"PORT\s*=\s*(\d+)" (str/trim line))]
                  (parse-port (nth m 1))))
              (str/split-lines content))))))

(defn port
  "Port to bind: PORT env var, then .env file, else default 3000."
  []
  (or (parse-port (System/getenv "PORT"))
      (port-from-env-file)
      default-port))
