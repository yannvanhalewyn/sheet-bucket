(ns sheet-bucket.controllers.sheets
  (:require [sheet-bucket.models.sheet :as sheet]
            [sheet-bucket.models.user :as user]
            [sheet-bucket.socket-handler :refer [socket-handler]]
            [datomic.api :as d]
            [clojure.walk :refer [postwalk]]))

(defmethod socket-handler :sheets/index
  [{:keys [?data ring-req]}]
  (user/sheets (:db-conn ring-req) (:user-id ?data)))

(defmethod socket-handler :sheets/show
  [{:keys [?data ring-req]}]
  (sheet/find (:db-conn ring-req) ?data))

(defmethod socket-handler :sheets/create
  [{:keys [?data ring-req]}]
  (sheet/create! (:db-conn ring-req) ?data))

(defmethod socket-handler :sheets/update
  [{:keys [?data ring-req]}]
  (let [{:keys [diff sheet-id]} ?data
        result (d/transact (:db-conn ring-req) (sheet/diff->tx diff sheet-id))]
    {:temp-ids (:tempids @result) :sheet-id sheet-id}))

(defmethod socket-handler :sheets/destroy
  [{:keys [?data ring-req]}]
  (let [result (d/transact (:db-conn ring-req) [[:db.fn/retractEntity ?data]])]
    {:success true :removed-id ?data}))
