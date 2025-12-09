(ns loan-market.bank
  (:require [loan-market.data-loader :as data-loader]
            [clojure.pprint :as pprint]))

(def sortBanks
  (fn [banks sort-by sort-direction]
    (let [key-fn (case sort-by
                   :name (fn [bank] (:name bank))
                   "name" (fn [bank] (:name bank))
                   :interest (fn [bank] (:interest bank))
                   "interest" (fn [bank] (:interest bank))
                   (throw (IllegalArgumentException.
                           (str "sortBy must be 'name' or 'interest', got: " sort-by))))
          sorted-banks (clojure.core/sort-by key-fn banks)]
      (if (or (= sort-direction :desc) (= sort-direction "desc"))
        (reverse sorted-banks)
        sorted-banks))))

(def find-highest-interest-bank
  "Uses reduce to find the bank with the highest interest rate"
  (fn [banks]
    (reduce (fn [max-bank current-bank]
              (if (> (:interest current-bank) (:interest max-bank))
                current-bank
                max-bank))
            (first banks)
            (rest banks))))

(def count-banks-above-threshold
  "Uses -> (thread-first) macro to chain operations: filter, map, and count"
  (fn [banks threshold]
    (->> banks
         (filter (fn [bank] (> (:interest bank) threshold)))
         (map :name)
         (count))))

(def format-bank-names
  "Uses map to transform bank data, adding formatted name with interest rate"
  (fn [banks]
    (map (fn [bank]
           (assoc bank
                  :formatted-name
                  (str (:name bank) " (" (:interest bank) "%)")))
         banks)))

(def -main
  (fn []
    (let [sorted-by-name (sortBanks data-loader/banks "name" "asc")
          sorted-by-interest (sortBanks data-loader/banks "interest" "asc")]
      (println "Sorted banks by name (asc):")
      (pprint/pprint sorted-by-name)
      (println "\nSorted banks by interest rate (asc):")
      (pprint/pprint sorted-by-interest)
      sorted-by-name)))

