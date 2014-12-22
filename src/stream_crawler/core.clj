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
   [twitter-streaming-client.core :as twitter-client])
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
(def stream (twitter-client/create-twitter-stream twitter.api.streaming/statuses-filter
                                          :oauth-creds twitter-creds
                                          :params {:follow "1640526475"}))

;;asynchronously call function with the :queues map from the TwitterStream
;; record, then reset the :queues map to empty

(defn create-database-objects [queues]
  "Takes a map of queues and creates the tweet database objects for each tweet
  in the queue"
  (println "-----")
  (println "=> Would create " (count (:tweet queues)) " tweets from queues")
  (println "-----")
  (def debug-mutable-queues queues))

(defn do-on-queues-changed [k, stream, os, nst]
  "k[ey], r[ef], o[ld]s[tate], n[ew]st[ate]
  do something when an object is added to queues"
  (let [buffered-tweets (:tweet (k nst))]
    (if (> (count buffered-tweets) 0)
      ; at least one tweet is in the queue
      (twitter-client/empty-queues stream create-database-objects))))


(defn -main []
  ; (twitter-client/cancel-twitter-stream stream)
  (twitter-client/start-twitter-stream stream)
  (add-watch stream :queues do-on-queues-changed))
