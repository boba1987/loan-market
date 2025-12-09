(ns loan-market.data-loader
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn load-banks-from-csv
  "Loads bank data from CSV file and returns a vector of bank maps"
  []
  (let [csv-file (io/file "resources/data/banks.csv")
        csv-content (slurp csv-file)
        all-lines (str/split-lines csv-content)
        data-lines (rest all-lines)]
    (vec (map (fn [line]
                (let [fields (str/split line #",")
                      id-str (nth fields 0)
                      name-str (nth fields 1)
                      interest-str (nth fields 2)
                      id (Integer/parseInt id-str)
                      interest (Double/parseDouble interest-str)]
                  {:id id
                   :name name-str
                   :interest interest}))
              data-lines))))

(def banks (load-banks-from-csv))

(defn add-bank-to-csv
  "Adds a new bank to the CSV file. Throws an error if a bank with the same id already exists.
   Parameters:
   - id: integer bank id
   - name: string bank name
   - interest: double interest rate"
  [id name interest]
  (let [csv-file (io/file "resources/data/banks.csv")
        csv-content (slurp csv-file)
        all-lines (str/split-lines csv-content)
        data-lines (rest all-lines)
        existing-ids (set (map (fn [line]
                                 (let [fields (str/split line #",")
                                       id-str (nth fields 0)]
                                   (Integer/parseInt id-str)))
                               data-lines))]
    (if (contains? existing-ids id)
      (throw (IllegalArgumentException.
              (str "Bank with id " id " already exists in the list")))
      (let [new-line (str id "," name "," interest)
            updated-lines (conj (vec all-lines) new-line)
            updated-content (str/join "\n" updated-lines)]
        (spit csv-file updated-content)))))

