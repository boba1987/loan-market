(ns loan-market.domain.data-loader
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

(defn list-banks
  "Return banks loaded from CSV (fresh each call)."
  []
  (load-banks-from-csv))

(defn delete-bank-from-csv
  "Delete a bank with given id from CSV. Throws ex-info if the bank doesn't exist."
  [id]
  (let [csv-file (io/file "resources/data/banks.csv")
        csv-content (slurp csv-file)
        all-lines (str/split-lines csv-content)
        header (first all-lines)
        data-lines (rest all-lines)
        remaining (filter (fn [line]
                             (let [fields (str/split line #",")
                                   id-str (nth fields 0)]
                               (not= (Integer/parseInt id-str) id)))
                           data-lines)
        removed-count (- (count data-lines) (count remaining))]
    (when (= removed-count 0)
      (throw (ex-info "Bank not found" {:id id})))
    (let [updated-content (str/join "\n" (cons header (vec remaining)))]
      (spit csv-file updated-content))))

(defn update-bank-in-csv
  "Update bank by id in CSV. Throws ex-info if the bank doesn't exist."
  [id name interest]
  (let [csv-file (io/file "resources/data/banks.csv")
        csv-content (slurp csv-file)
        all-lines (str/split-lines csv-content)
        header (first all-lines)
        data-lines (rest all-lines)
        updated-data (map (fn [line]
                             (let [fields (str/split line #",")
                                   id-str (nth fields 0)]
                               (if (= (Integer/parseInt id-str) id)
                                 (str id "," name "," interest)
                                 line)))
                           data-lines)
        changed? (some (fn [line]
                          (let [fields (str/split line #",")
                                id-str (nth fields 0)]
                            (= (Integer/parseInt id-str) id)))
                        updated-data)]
    (when-not changed?
      (throw (ex-info "Bank not found" {:id id})))
    (let [updated-content (str/join "\n" (cons header (vec updated-data)))]
      (spit csv-file updated-content))))
