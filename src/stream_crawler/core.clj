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
   [environ.core :refer [env]])
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

(def ^:dynamic
     *custom-streaming-callback*
     (AsyncStreamingCallback. #(println % %2)
                      (comp println response-return-everything)
                  exception-print))

(defn -main []
  (statuses-filter :params {:track "WeAreAllHarry"}
         :oauth-creds twitter-creds
         :callbacks *custom-streaming-callback*))
