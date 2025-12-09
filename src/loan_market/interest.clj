(ns loan-market.interest
  (:require [loan-market.data-loader :as data-loader]))

(def average-interest-rate
  (fn
    ([]
     (average-interest-rate data-loader/banks))
    ([banks]
     (if (empty? banks)
       0.0
       (let [rates (map :interest banks)
             sum (reduce + rates)
             count (count rates)]
         (/ sum count))))))

(def total-interest
  "Uses reduce to calculate the sum of all interest rates"
  (fn
    ([]
     (total-interest data-loader/banks))
    ([banks]
     (reduce (fn [sum bank]
               (+ sum (:interest bank)))
             0
             banks))))

(def banks-above-average-count
  "Uses -> (thread-first) macro to filter banks above average, then count them"
  (fn
    ([]
     (banks-above-average-count data-loader/banks))
    ([banks]
     (let [avg (average-interest-rate banks)]
       (->> banks
            (filter (fn [bank] (> (:interest bank) avg)))
            (count))))))

(def interest-rates-above-threshold
  "Uses map to extract interest rates above a given threshold"
  (fn
    ([threshold]
     (interest-rates-above-threshold data-loader/banks threshold))
    ([banks threshold]
     (map :interest
          (filter (fn [bank] (> (:interest bank) threshold))
                  banks)))))

(def -main
  (fn []
    (let [avg-rate (average-interest-rate)]
      (println "Average interest rate:" avg-rate "%")
      avg-rate)))
