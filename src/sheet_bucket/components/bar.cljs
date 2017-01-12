(ns sheet-bucket.components.bar
  (:require [sheet-bucket.components.chord :refer [displayed-chord]]))

(def style {:display "flex"
            :justify-content "space-between"})

(defn component [{:keys [chords]}]
  [:div
   {:style (merge style (when (> (count chords) 1) {:border-bottom "2px solid black"}))}
   (map-indexed (fn [i chord] ^{:key i} [displayed-chord chord]) chords)])