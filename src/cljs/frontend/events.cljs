(ns frontend.events
  (:require [frontend.selectors :as selectors]
            [frontend.fx :refer [reg-event-db reg-event-fx]]
            [frontend.router :as router]
            [frontend.reducer :as reducer]
            [frontend.models.sheet-zip :as sheet-zip]
            [frontend.models.sheet :as sheet]
            [frontend.models.attachment :as attachment]
            [shared.utils :as sutil]
            [clojure.zip :as zip]
            [shared.datsync :as datsync]))

(reg-event-db
  :app/init
  (fn [_ event]
    (reducer/app nil event)))

;; Editor operations
;; =================

(reg-event-db
  :sheet/deselect
  (fn [db event] (reducer/app db event)))

(reg-event-db
  :sheet/select
  (fn [db event]
    (reducer/app db event)))

(reg-event-fx
  :sheet/update-chord
  (fn [{:keys [db]} [_ id value next]]
    (let [tx (sheet/update-chord (:db/sheets db) id value)]
      (if next
        {:datsync tx :dispatch next}
        {:datsync tx}))))

(reg-event-fx
  :sheet/append
  (fn [{:keys [db]} [_ type]]
    (let [chord-id (:selection/id (selectors/selection db))
          tx (sheet/append (:db/sheets db) type chord-id)]
      {:datsync tx})))

(reg-event-db
  :sheet/move
  (fn [db [_ dir]]
    (if-let [loc (sheet-zip/move (selectors/current-loc db) dir)]
      (reducer/app db [:sheet/move (:db/id (zip/node loc))])
      db)))

(reg-event-fx
  :sheet/remove
  (fn [{:keys [db]} [_ element]]
    (let [chord-id (:selection/id (selectors/selection db))
          tx (sheet/delete (:db/sheets db) element chord-id)
          loc (sheet-zip/nearest-chord (selectors/current-loc db))]
      {:db (reducer/app db [:sheet/move (:db/id (zip/node loc))])
       :datsync tx})))

(reg-event-fx
  :sheet/set-artist
  (fn [{:keys [db]} [_ sheet-id artist]]
    {:datsync [[:db/add sheet-id :sheet/artist artist]]}))

(reg-event-fx
  :sheet/set-title
  (fn [{:keys [db]} [_ sheet-id title]]
    {:datsync [[:db/add sheet-id :sheet/title title]]}))

(reg-event-fx
  :sheet/set-section-title
  (fn [{:keys [db]} [_ section title]]
    {:datsync [[:db/add (:db/id section) :section/title title]]}))

;; Attachments and symbol operations
;; =================================

(reg-event-fx
  :sheet/remove-selection
  (fn [{:keys [db]} e]
    (if-let [{:keys [selection/id]} (selectors/selection db)]
      {:datsync [[:db.fn/retractEntity id]]})))

(reg-event-fx
  :sheet/edit-textbox
  (fn [{:keys [db]} [_ textbox-id value]]
    {:datsync (attachment/set-value (:db/sheets db) textbox-id value)}))

(reg-event-fx
  :sheet/create-or-update-time-signature
  (fn [{:keys [db]} [e bar-id time-signature]]
    {:dispatch [:modal/close]
     :datsync (attachment/create-or-update-time-signature
                (:db/sheets db) bar-id time-signature)}))

(reg-event-fx
  :sheet/remove-time-signature
  (fn [{:keys [db]} [e time-signature-id]]
    {:dispatch [:modal/close]
     :datsync [[:db.fn/retractEntity time-signature-id]]}))

(reg-event-fx
  :sheet/add-bar-attachment
  (fn [{:keys [db]} [_ type]]
    (if-let [loc (selectors/current-loc db)]
      (let [bar-id (-> loc zip/up zip/node :db/id)]
        {:datsync (attachment/add (:db/sheets db) bar-id type)}))))

(reg-event-fx
  :sheet/add-chord-attachment
  (fn [{:keys [db]} [_ type]]
    (if-let [chord-id (:selection/id (selectors/selection db))]
      {:datsync (attachment/add (:db/sheets db) chord-id type)})))

(reg-event-fx
  :sheet/set-repeat-cycle
  (fn [{:keys [db]} [_ bar-id value]]
    {:datsync (attachment/set-repeat-cycle (:db/sheets db) bar-id value)}))

(reg-event-fx
  :sheet/move-symbol
  (fn [{:keys [db]} [_ att-id pos]]
    {:datsync (attachment/move (:db/sheets db) att-id pos)}))

;; Playlist actions
;; ================

(reg-event-fx
  :playlist/create-sheet
  (fn [db [_ owner-id]]
    {:socket {:create-sheet [:sheets/create owner-id]}}))

(reg-event-fx
  :playlist/destroy-sheet
  (fn [db [_ sheet-id]]
    {:datsync [[:db.fn/retractEntity sheet-id]]}))

;; App flow
;; ========

(reg-event-db :modal/show reducer/app)
(reg-event-db :modal/close reducer/app)

;; Remote actions
;; ==============

(defn reg-events-remote [key]
  (doseq [type #{:request :response :response.failure}]
    (reg-event-db (keyword (name type) (name key))
      (fn [db event] (reducer/app db event)))))

(reg-events-remote :get-sheet)
(reg-events-remote :get-sheets)
(reg-events-remote :sync-sheet)
(reg-events-remote :get-current-user)
(reg-events-remote :create-sheet)
(reg-events-remote :destroy-sheet)
(reg-events-remote :datsync)

(reg-event-fx :chsk/state
  (fn [_ [_ [_ state]]]
    (when (:first-open? state)
      {:socket {:get-current-user [:users/me]}})))

(reg-event-fx :chsk/handshake (fn [_ _]))

(reg-event-db
  :chsk/recv
  (fn [db [_ [tx datoms]]]
    (if (= :sheet/tx-data tx)
      (reducer/app db [:chsk/incoming-tx-data datoms])
      db)))

(reg-event-db
  :route/browser-url
  (fn [db event] (reducer/app db event)))
