(ns loan-market.bank-test
  (:require [midje.sweet :refer [facts fact => throws]]
            [loan-market.bank :as bank]))

(facts "about sortBanks"
       (let [test-banks [{:name "Bank A" :interest 2.5}
                         {:name "Bank B" :interest 3.0}
                         {:name "Bank C" :interest 1.5}]]

         (fact "sorts banks by name in ascending order"
               (bank/sortBanks test-banks "name" "asc") => [{:name "Bank A" :interest 2.5}
                                                            {:name "Bank B" :interest 3.0}
                                                            {:name "Bank C" :interest 1.5}])

         (fact "sorts banks by name in descending order"
               (bank/sortBanks test-banks "name" "desc") => [{:name "Bank C" :interest 1.5}
                                                             {:name "Bank B" :interest 3.0}
                                                             {:name "Bank A" :interest 2.5}])

         (fact "sorts banks by interest in ascending order"
               (bank/sortBanks test-banks "interest" "asc") => [{:name "Bank C" :interest 1.5}
                                                                {:name "Bank A" :interest 2.5}
                                                                {:name "Bank B" :interest 3.0}])

         (fact "sorts banks by interest in descending order"
               (bank/sortBanks test-banks "interest" "desc") => [{:name "Bank B" :interest 3.0}
                                                                 {:name "Bank A" :interest 2.5}
                                                                 {:name "Bank C" :interest 1.5}])

         (fact "accepts keyword arguments for sort-by"
               (bank/sortBanks test-banks :name :asc) => [{:name "Bank A" :interest 2.5}
                                                          {:name "Bank B" :interest 3.0}
                                                          {:name "Bank C" :interest 1.5}])

         (fact "throws exception for invalid sort-by"
               (bank/sortBanks test-banks "invalid" "asc") => (throws IllegalArgumentException))))

(facts "about find-highest-interest-bank"
       (let [test-banks [{:name "Bank A" :interest 2.5}
                         {:name "Bank B" :interest 3.0}
                         {:name "Bank C" :interest 1.5}]]

         (fact "finds the bank with the highest interest rate"
               (bank/find-highest-interest-bank test-banks) => {:name "Bank B" :interest 3.0})

         (fact "handles single bank"
               (bank/find-highest-interest-bank [{:name "Bank A" :interest 2.5}]) => {:name "Bank A" :interest 2.5})

         (fact "handles banks with equal interest rates"
               (bank/find-highest-interest-bank [{:name "Bank A" :interest 2.5}
                                                 {:name "Bank B" :interest 2.5}]) => {:name "Bank A" :interest 2.5})))

(facts "about count-banks-above-threshold"
       (let [test-banks [{:name "Bank A" :interest 2.5}
                         {:name "Bank B" :interest 3.0}
                         {:name "Bank C" :interest 3.5}
                         {:name "Bank D" :interest 1.5}]]

         (fact "counts banks above threshold"
               (bank/count-banks-above-threshold test-banks 3.0) => 1)

         (fact "counts banks above lower threshold"
               (bank/count-banks-above-threshold test-banks 2.0) => 3)

         (fact "returns zero when no banks are above threshold"
               (bank/count-banks-above-threshold test-banks 5.0) => 0)

         (fact "handles empty bank list"
               (bank/count-banks-above-threshold [] 3.0) => 0)))

(facts "about format-bank-names"
       (let [test-banks [{:name "Bank A" :interest 2.5}
                         {:name "Bank B" :interest 3.0}]]

         (fact "formats bank names with interest rates"
               (bank/format-bank-names test-banks) => [{:name "Bank A" :interest 2.5 :formatted-name "Bank A (2.5%)"}
                                                       {:name "Bank B" :interest 3.0 :formatted-name "Bank B (3.0%)"}])

         (fact "handles empty bank list"
               (bank/format-bank-names []) => [])))

