(ns loan-market.bank
  (:require [loan-market.mock-data :as mock-data]))

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

(def -main
  (fn []
    (let [sorted-by-name (sortBanks mock-data/banks "name" "asc")
          sorted-by-interest (sortBanks mock-data/banks "interest" "asc")]
      (println "Sorted banks by name (asc):" sorted-by-name)
      (println "Sorted banks by interest rate (asc):" sorted-by-interest)
      sorted-by-name)))

