(ns frontend.models.sheet-2-test
  (:require [frontend.models.sheet-2 :as sut]
            [cljs.test :as t :refer-macros [deftest testing is are]]
            [datascript.core :as d]))

(def BLANK_SHEET
  {:db/id 1
   :sheet/title "Title"
   :sheet/artist "Artist"
   :sheet/sections {:db/id 2
                    :section/title "Intro"
                    :coll/position 0
                    :section/rows {:db/id 3
                                   :coll/position 0
                                   :row/bars {:db/id 4
                                              :coll/position 0
                                              :bar/chords {:db/id 5
                                                           :coll/position 0
                                                           :chord/value ""}}}}})

(def db (let [conn (d/create-conn sut/schema)]
          (d/transact! conn [BLANK_SHEET])
          @conn))

(defn- tx-apply [db tx-fn & args]
  (:db-after (d/with db (apply tx-fn db args))))

(deftest update-chord
  (is (= "Ab" (:chord/value (d/entity (tx-apply db sut/update-chord 5 "Ab") 5)))))

(deftest append
  (testing "Append chords at the end"
    (let [db (tx-apply db sut/append :chord 5)]
      (is (= [{:chord/value "" :coll/position 0}
              {:chord/value "" :coll/position 1}]
            (map #(into {} %) (:bar/chords (d/entity db 4)))))))

  (testing "Append chord in between"
    (let [db (-> db
               (tx-apply sut/update-chord 5 "first")
               (tx-apply sut/append :chord 5)
               (tx-apply sut/update-chord 6 "last")
               (tx-apply sut/append :chord 5)
               (tx-apply sut/update-chord 7 "middle"))]
      (is (= [{:chord/value "first" :coll/position 0}
              {:chord/value "last" :coll/position 2}
              {:chord/value "middle" :coll/position 1}]
            (map #(into {} %) (:bar/chords (d/entity db 4))))))))
