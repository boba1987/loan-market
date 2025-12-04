(ns loan-market.interest
  (:require [loan-market.mock-data :as mock-data]))

(def average-interest-rate
  (fn
    ([]
     (average-interest-rate mock-data/banks))
    ([banks]
     (if (empty? banks)
       0.0
       (let [rates (map :interest banks)
             sum (reduce + rates)
             count (count rates)]
         (/ sum count))))))

(def -main
  (fn []
    (let [avg-rate (average-interest-rate)]
      (println "Average interest rate:" avg-rate "%")
      avg-rate)))
