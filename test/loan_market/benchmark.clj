(ns loan-market.benchmark
  (:require [criterium.core :as c]
            [loan-market.bank :as bank]
            [loan-market.interest :as interest]
            [loan-market.data-loader :as data-loader]))

(def test-banks [{:name "Bank A" :interest 2.5}
                 {:name "Bank B" :interest 3.0}
                 {:name "Bank C" :interest 1.5}
                 {:name "Bank D" :interest 4.0}
                 {:name "Bank E" :interest 2.8}])

(defn benchmark-sort-banks []
  (println "\n=== Benchmarking sortBanks ===")
  (c/bench (bank/sortBanks test-banks "name" "asc")))

(defn benchmark-find-highest-interest-bank []
  (println "\n=== Benchmarking find-highest-interest-bank ===")
  (c/bench (bank/find-highest-interest-bank test-banks)))

(defn benchmark-count-banks-above-threshold []
  (println "\n=== Benchmarking count-banks-above-threshold ===")
  (c/bench (bank/count-banks-above-threshold test-banks 3.0)))

(defn benchmark-format-bank-names []
  (println "\n=== Benchmarking format-bank-names ===")
  (c/bench (bank/format-bank-names test-banks)))

(defn benchmark-average-interest-rate []
  (println "\n=== Benchmarking average-interest-rate ===")
  (c/bench (interest/average-interest-rate test-banks)))

(defn benchmark-total-interest []
  (println "\n=== Benchmarking total-interest ===")
  (c/bench (interest/total-interest test-banks)))

(defn benchmark-banks-above-average-count []
  (println "\n=== Benchmarking banks-above-average-count ===")
  (c/bench (interest/banks-above-average-count test-banks)))

(defn benchmark-interest-rates-above-threshold []
  (println "\n=== Benchmarking interest-rates-above-threshold ===")
  (c/bench (interest/interest-rates-above-threshold test-banks 3.0)))

(defn benchmark-all []
  "Run all benchmarks"
  (println "Running performance benchmarks on loan-market functions...")
  (benchmark-sort-banks)
  (benchmark-find-highest-interest-bank)
  (benchmark-count-banks-above-threshold)
  (benchmark-format-bank-names)
  (benchmark-average-interest-rate)
  (benchmark-total-interest)
  (benchmark-banks-above-average-count)
  (benchmark-interest-rates-above-threshold)
  (println "\n=== All benchmarks completed ==="))

(defn -main []
  (benchmark-all))

