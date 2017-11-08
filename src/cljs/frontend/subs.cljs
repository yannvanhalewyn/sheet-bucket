(ns frontend.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub :sub/sheet (fn [db] (:db/sheet db)))
(reg-sub :sub/selected (fn [db] (:db/selected db)))