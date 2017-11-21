(ns sheet-bucket.core
  (:require [com.stuartsierra.component :as c]
            [sheet-bucket.components.channel-sockets :as chsk]
            [sheet-bucket.components.db :as db]
            [sheet-bucket.components.web :as web]))

(defn new-system [config]
  (c/system-map
    :db (db/component (:db-url config))
    :web (c/using (web/component (:port config)) [:db :channel-sockets])
    :channel-sockets (chsk/component)))
