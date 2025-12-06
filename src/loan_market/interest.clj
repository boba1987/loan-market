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

(def -main
  (fn []
    (let [avg-rate (average-interest-rate)]
      (println "Average interest rate:" avg-rate "%")
      avg-rate)))
