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
   [clj-time.format :as tf]
   [clojure.tools.logging :as log]
   [clojure.string :as str])
  (:import
   (twitter.callbacks.protocols AsyncStreamingCallback)))

(defn -main []
  (def db-creds {:db (env :db-name)
                 :user (env :db-user)
                 :password (env :db-pass)
                 :host (env :db-url)
                 :port (env :db-port)})
  (def twitter-creds
    (make-oauth-creds (env :twitter-consumer-key)
                      (env :twitter-consumer-secret)
                      (env :user-access-token)
                      (env :user-access-token-secret)))

  (log/info "using db creds: " db-creds)
  (log/info "using twitter creds: " twitter-creds)

  (defdb db (postgres db-creds))

  (def serialized-keywords
    (str/join "," (map :keyword
      (select keyword-objects
        (fields :keyword)
        (where {:type "PositiveKeyword"})))))

  (def stream
    (twitter-client/create-twitter-stream twitter.api.streaming/statuses-filter
      :oauth-creds twitter-creds
      :params {:track serialized-keywords}))

  (defn commit-tweet-queue-to-database [queues]
    "Takes a map of queues and creates the tweet database objects for each tweet
    in the queue"
    (let [tweet-queue queues]
      ; (def mut-debug-queues queues)
      ; (println "wrote queues!")
      (doseq [tweet (:tweet tweet-queue)] (create-tweet-entities tweet))))

  (defn do-on-queues-changed [k, stream, os, nst]
    (let [buffered-tweets (:tweet (k nst))]
      (if (> (count buffered-tweets) 0)
        ; at least one tweet is in the queue
        (do (log/info "emptying queue and attempting to commit tweet")
            (twitter-client/empty-queues stream commit-tweet-queue-to-database)))))


  (log/info (str "Starting stream client at " (t/now)))
  (twitter-client/start-twitter-stream stream)
  (add-watch stream :queues do-on-queues-changed))

; (twitter-client/cancel-twitter-stream stream)

