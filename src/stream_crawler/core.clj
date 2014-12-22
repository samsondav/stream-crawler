(ns stream-crawler.core
  (:gen-class)
  (:use
   [twitter.oauth]
   [twitter.callbacks]
   [twitter.callbacks.handlers]
   [twitter.api.streaming]
   [korma.core]
   [korma.db])
  (:require
   [clojure.data.json :as json]
   [http.async.client :as ac]
   [clj-time.core :as t]
   [clj-time.coerce :as tc]
   [environ.core :refer [env]]
   [twitter-streaming-client.core :as client])
  (:import
   (twitter.callbacks.protocols AsyncStreamingCallback)))

(def twitter-creds (make-oauth-creds (env :twitter-consumer-key)
                                (env :twitter-consumer-secret)
                                (env :user-access-token)
                                (env :user-access-token-secret)))

(defdb db (postgres {:db (env :db-name)
                     :user (env :db-user)
                     :password (env :db-pass)}))

(defentity tweets)

;; create the client with a twitter.api streaming method
(def stream (client/create-twitter-stream twitter.api.streaming/statuses-filter
                                          :oauth-creds twitter-creds
                                          :params {:follow "1640526475"}))

;;asynchronously call function with the :queues map from the TwitterStream
;; record, then reset the :queues map to empty


(defn do-on-queues-changed [k, r, os, nst]
  "k[ey], r[ef], o[ld]s[tate], n[ew]st[ate]
  do something when an object is added to queues"
  (println "-----")
  (println (str "=> Key: " k))
  (println (str "=> Ref: " r))
  (println (str "=> OldState: " os))
  (println (str "=> NewState: " nst))
  (println (str "=> Old queue: " (k os)))
  (println (str "=> New queue: " (k nst))))

(defn -main []
  ; (client/cancel-twitter-stream stream))
  (client/start-twitter-stream stream)
  (add-watch stream :queues do-on-queues-changed))
