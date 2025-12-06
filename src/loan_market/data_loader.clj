(ns loan-market.data-loader
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

(defn load-banks-from-csv
  "Loads bank data from CSV file and returns a vector of bank maps"
  []
  (let [csv-file (io/file "resources/data/banks.csv")
        csv-content (slurp csv-file)
        csv-data (csv/read-csv (java.io.StringReader. csv-content))
        rows (->> (rest csv-data)
                  (filter (fn [row] (not (every? empty? row)))))]
    (vec (map (fn [row]
                {:id (Integer/parseInt (nth row 0))
                 :name (nth row 1)
                 :interest (Double/parseDouble (nth row 2))})
              rows))))

(def banks (load-banks-from-csv))

