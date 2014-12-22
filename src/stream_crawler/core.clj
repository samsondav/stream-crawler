(ns stream-crawler.core
  (:gen-class)
  (:use
   [twitter.oauth]
   [twitter.callbacks]
   [twitter.callbacks.handlers]
   [twitter.api.streaming]
   [korma.core]
   [korma.db]
   [stream-crawler.impl])
  (:require
   [clojure.data.json :as json]
   [http.async.client :as ac]
   [clj-time.core :as t]
   [clj-time.coerce :as tc]
   [environ.core :refer [env]]
   [twitter-streaming-client.core :as twitter-client]
   [clojure.data.json :as json]
   [clj-time.format :as tf])
  (:import
   (twitter.callbacks.protocols AsyncStreamingCallback)))

(def twitter-creds (make-oauth-creds (env :twitter-consumer-key)
                                (env :twitter-consumer-secret)
                                (env :user-access-token)
                                (env :user-access-token-secret)))

(defdb db (postgres {:db (env :db-name)
                     :user (env :db-user)
                     :password (env :db-pass)}))

(defentity stream-tweets
  (table :twitter_stream_dump))

;; create the client with a twitter.api streaming method
(def stream (twitter-client/create-twitter-stream twitter.api.streaming/statuses-filter
                                          :oauth-creds twitter-creds
                                          :params {:follow "1640526475"}))

(defn create-tweet-entities [twitter-tweet]
  ; the entirety of this method should be wrapped in a database transaction
  (println (str "=> Creating tweet id: " (:id twitter-tweet)))
  (insert stream-tweets
    (values (twitter-tweet-to-stream-tweet twitter-tweet))))

(defn commit-tweet-queue-to-database [queues]
  "Takes a map of queues and creates the tweet database objects for each tweet
  in the queue"
  (let [tweet-queue queues]
    (doseq [tweet (:tweet tweet-queue)] (create-tweet-entities tweet))))

(defn do-on-queues-changed [k, stream, os, nst]
  (let [buffered-tweets (:tweet (k nst))]
    (if (> (count buffered-tweets) 0)
      ; at least one tweet is in the queue
      (do (println "=> Emptying queue and attempting to commit tweet")
          (twitter-client/empty-queues stream commit-tweet-queue-to-database)))))


(defn -main []
  ; (twitter-client/cancel-twitter-stream stream)
  (twitter-client/start-twitter-stream stream)
  (add-watch stream :queues do-on-queues-changed))
