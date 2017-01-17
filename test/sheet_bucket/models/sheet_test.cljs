(ns sheet-bucket.models.sheet-test
  (:require [sheet-bucket.models.sheet
             :refer [new-sheet zipper navigate-to append]
             :as sheet]
            [cljs.test :refer-macros [deftest is]]
            [goog.string :refer [format]]
            [clojure.zip :refer [node up left down children]]))

(def test-loc (-> new-sheet zipper (navigate-to "1")))

(deftest navigateTo
  (is (= "1" (-> new-sheet zipper (navigate-to "1") node :id)))
  (is (= nil (-> new-sheet zipper (navigate-to "2"))))
  (is (= "1" (-> new-sheet zipper (navigate-to "1") (append :chord "2") (navigate-to "1") node :id))))

(deftest addChord
  (let [new-chord (-> test-loc (append :chord "2"))]
    (is (= 2 (-> new-chord up children count)))
    (is (= "2" (-> new-chord node :id)))))

(deftest addBar
  (let [new-chord (-> test-loc (append :bar "2"))]
    (is (= 2 (-> new-chord up up children count)))
    (is (= "2" (-> new-chord node :id)))))

(deftest addRow
  (let [new-chord (-> test-loc (append :row "2"))]
    (is (= 2 (-> new-chord up up up children count)))
    (is (= "2" (-> new-chord node :id)))))

(deftest addSection
  (let [new-chord (-> test-loc (append :section "2"))]
    (is (= 2 (-> new-chord up up up up children count)))
    (is (= "2" (-> new-chord node :id)))))



(deftest move
  (let [sheet (-> test-loc
                  (append :chord "2") (append :bar "3")
                  (append :row "4") (append :bar "5")
                  (append :row "6")
                  (append :row "7") (append :bar "8") (append :bar "9") (append :chord "10")
                  (append :section "11") (append :bar "12")
                  (navigate-to "1"))
        check (fn [moves expected]
                (let [land (reduce
                            #(let [move (if (string? %2) sheet/navigate-to sheet/move)]
                               (move %1 %2))
                            sheet moves)]
                  (is land (str "Couldn't find element for moves: " moves))
                  (is (= expected (-> land node :id))
                      (format "Failed move test for %s. Expected: %s, got: %s"
                              moves expected (-> land node :id)))))]
    ;; Testing the moves in a sheet.
    ;; |-----+----+------|
    ;; | 1 2 | 3  |      |
    ;; | 4   | 5  |      |
    ;; | 6   |    |      |
    ;; | 7   | 8  | 9 10 |
    ;; |-----+----+------|
    ;; | 11  | 12 |      |

    ;; Basics
    ;; ======
    (check [:right] "2")
    (check [:right :left] "1")
    (check [:right :bar-left] "1")
    (check [:bar-right] "3")
    (check [:down :up] "1")
    (check [:bar-right :down] "5")
    (check [:bar-right :left] "2")
    (check ["7" :down] "11")
    (check ["11" :up] "7")

    ;; Make a little circle for sanity
    ;; ===============================
    (check [:down] "4")
    (check [:down :bar-right] "5")
    (check [:down :bar-right :up] "3")
    (check [:down :bar-right :up :bar-left] "1")

    ;; Vertical jumping to latest bar
    ;; ==============================
    (check ["5" :down] "6")
    (check ["8" :up] "6")
    (check ["9" :up] "6")
    (check ["3" :right] "4")

    ;; Wrap arounds
    ;; ============
    (check ["4" :left] "3")
    (check ["4" :bar-left] "3")
    (check ["3" :bar-right] "4")

    ;; Section wrap arounds
    ;; ====================
    (check ["9" :bar-right] "11")
    (check ["10" :right] "11")
    (check ["11" :left] "10")
    (check ["11" :bar-left] "9")
    (check ["9" :down] "12")

    ;; Out of bounds
    ;; =============
    (check [:up] "1")
    (check [:left] "1")
    (check [:bar-left] "1")
    (check ["12" :right] "12")
    (check ["12" :bar-right] "12")
    (check ["11" :down] "11")
    (check ["12" :down] "12")))
