(ns stream-crawler.impl
  (:require
   [clj-time.core :as t]
   [clj-time.coerce :as tc]
   [clj-time.format :as tf])
  (:gen-class))

(defn retweet? [tw-tweet] (not (nil? (:retweeted_status tw-tweet))))

(defn reply? [tw-tweet] (not (nil? (:in_reply_to_status_id tw-tweet))))

(defn tweet-type-of [tw-tweet]
  (cond
    (reply? tw-tweet) 2
    (retweet? tw-tweet) 1
    :else 0))

(def parse-twitter-utc-string #(tf/parse (tf/formatter "E MMM dd HH:mm:ss Z YYYY") %))

(defn twitter-tweet-to-stream-tweet [tw-tweet]
  "renames keys of a twitter tweet map to a set of values for inserting to the
  twitter-stream-dump database"
  {:author_username (:screen_name (:user tw-tweet))
   :author_name (:name (:user tw-tweet))
   :body (:text tw-tweet)
   :latitude (if-let [coords (:coordinates tw-tweet)] (nth (:coordinates coords) 1))
   :longitude (if-let [coords (:coordinates tw-tweet)] (nth (:coordiantes coords) 0))
   :source (:source tw-tweet)
   :publication_time (tc/to-sql-time (parse-twitter-utc-string (:created_at tw-tweet)))
   :created_at (tc/to-sql-time (t/now))
   :updated_at (tc/to-sql-time (t/now)) ; here we are both creating and updating
   :id (:id tw-tweet)
   :tweet_author_id (:id (:user tw-tweet))
   :impressions_count (:followers_count (:user tw-tweet))
   :retweet_count (:retweet_count tw-tweet)
   :tweet_type (tweet-type-of tw-tweet)
   :rt_retweet_of_user_id (if (retweet? tw-tweet) (:id (:user (:retweeted_status tw-tweet))) -1)
   :rt_retweet_of_tweet_id (if (retweet? tw-tweet) (:id (:retweeted_status tw-tweet)) -1)
   :rt_retweet_of_username (if (retweet? tw-tweet) (:screen_name (:user (:retweeted_status tw-tweet))) "")
   :rp_in_reply_to_tweet_id (if (reply? tw-tweet) (:in_reply_to_status_id tw-tweet) -1)
   :rp_in_reply_to_user_id (if (reply? tw-tweet) (:in_reply_to_user_id tw-tweet) -1)
   :rp_in_reply_to_username (if (reply? tw-tweet) (:in_reply_to_screen_name tw-tweet) "")
   :favorite_count (:favorite_count tw-tweet)
   :lang (:lang tw-tweet) })
