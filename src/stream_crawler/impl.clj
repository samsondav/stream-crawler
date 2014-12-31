(ns stream-crawler.impl
  (:use
   [korma.core]
   [korma.db]
   [alex-and-georges.debug-repl])
  (:require
   [clj-time.core :as t]
   [clj-time.coerce :as tc]
   [clj-time.format :as tf]
   [clojure.tools.logging :as log]
   [naan.core :as naan]
   [naan.korma-helpers :as helpers])
  (:gen-class))

(declare hashtags cloud-words urls tweets tweet-authors keyword-objects url-in-tweet)

(defentity hashtags
  (helpers/attributes :id :tweet_id :hashtag)
  (helpers/entity-fields-from-attributes)
  (table :hashtags)
  (belongs-to tweets))

(defentity cloud-words
  (helpers/attributes :id :updated_at :created_at)
  (helpers/entity-fields-from-attributes)
  (table :cloud_words)
  (belongs-to tweets))

(defentity urls
  (helpers/attributes :id :extended_url :updated_at :created_at)
  (helpers/entity-fields-from-attributes)
  (pk :id)
  (table :urls)
  (many-to-many tweets :tweets_urls))

(defentity tweet-sentiment
  (helpers/attributes :id :tweet_id :updated_at :created_at)
  (helpers/entity-fields-from-attributes)
  (table :tweet_sentiments)
  (belongs-to tweets))

(defentity tweets
  (helpers/attributes :id :updated_at :created_at)
  (helpers/entity-fields-from-attributes)
  (table :tweets)
  (has-many hashtags)
  (has-many cloud-words)
  (has-one tweet-sentiment)
  (many-to-many urls :tweets_urls))

(defentity tweet-authors
  (helpers/attributes :id :updated_at :created_at)
  (helpers/entity-fields-from-attributes)
  (table :tweet_authors)
  (has-many tweets))

(defentity keyword-objects
  (helpers/attributes :id :updated_at :created_at)
  (helpers/entity-fields-from-attributes)
  (table :keywords))

(defentity url-in-tweets
  (table :tweets_urls))

(defn retweet? [tw-tweet] (not (nil? (:retweeted_status tw-tweet))))

(defn reply? [tw-tweet] (not (nil? (:in_reply_to_status_id tw-tweet))))

(defn get-tweet-type [tw-tweet]
  (cond
    (reply? tw-tweet) 2
    (retweet? tw-tweet) 1
    :else 0))

(defn get-parent [tw-tweet]
  (if-let [parent (:retweeted_status tw-tweet)]
    parent
    nil))

(defn find-or-create-by [entity attributes]
  "Attempts to find an entity using the provided attributes. If no matching
   entity is found, creates a new one and returns that."
  (if-let [existing (naan/read entity attributes)]
    existing
    (naan/create entity attributes)))

(def parse-twitter-utc-string #(tf/parse (tf/formatter "E MMM dd HH:mm:ss Z YYYY") %))

(defn twitter-tweet-to-db-tweet [tw-tweet]
  "renames keys of a twitter tweet map to a set of values for inserting to the
  twitter-stream-dump database"
  ; TODO rename for insertion to tweets
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
   :tweet_type (get-tweet-type tw-tweet)
   :rt_retweet_of_user_id (if (retweet? tw-tweet) (:id (:user (:retweeted_status tw-tweet))) -1)
   :rt_retweet_of_tweet_id (if (retweet? tw-tweet) (:id (:retweeted_status tw-tweet)) -1)
   :rt_retweet_of_username (if (retweet? tw-tweet) (:screen_name (:user (:retweeted_status tw-tweet))) "")
   :rp_in_reply_to_tweet_id (if (reply? tw-tweet) (:in_reply_to_status_id tw-tweet) -1)
   :rp_in_reply_to_user_id (if (reply? tw-tweet) (:in_reply_to_user_id tw-tweet) -1)
   :rp_in_reply_to_username (if (reply? tw-tweet) (:in_reply_to_screen_name tw-tweet) "")
   :favorite_count (:favorite_count tw-tweet)
   :lang (:lang tw-tweet) })

(defn build-tweet-author [tw-tweet]
  "Builds map of a twitter tweet user object for insertion to the tweet_author
   database"
   (let [tw-author (:user tw-tweet)]
     {:id (:id tw-author)
      :friendly_name (:name tw-author)
      :username (:screen_name tw-author)
      :description (:description tw-author)
      :url (:url tw-author)
      :location (:location tw-author)
      :follower_count (:followers_count tw-author)
      :following_count (:friends_count tw-author)
      :publication_count (:statuses_count tw-author)
      :time_zone (:time_zone tw-author)
      :created_at (tc/to-sql-time (t/now))
      :updated_at (tc/to-sql-time (t/now))
      :contributors_enabled (:contributors_enabled tw-author)
      :default_profile_image (:default_profile_image tw-author)
      :favourites_count (:favourites_count tw-author)
      :geo_enabled (:geo_enabled tw-author)
      :lang (:lang tw-author)
      :listed_count (:listed_count tw-author)
      :profile_image_url (:profile_image_url tw-author) }))

(defn build-hashtags [tw-tweet]
  "Build map[s] of hashtag[s] in tweet, including the tweet_id.
   Returns an empty array if none are found"
  (let [hashtags (:hashtags (:entities tw-tweet))
        tw-id (:id tw-tweet)]
    (mapv
      (fn [tw-ht] {:hashtag (str "#" (:text tw-ht)) :tweet_id tw-id})
      hashtags )))

(defn build-urls [tw-tweet]
  "Build map[s] of url[s] in tweet. Returns an empty sequence if none are found."
  (let [urls (:urls (:entities tw-tweet))]
    (for [tw-url urls]
      {:extended_url (:expanded_url tw-url)})))

(defn build-media [tw-tweet]
  "Build map[s] of media entity[s] in tweet. Returns an empty sequence if none
   are found."
  (let [medias (:media (:entities tw-tweet))]
    (for [media medias]
      {:extended_url (:expanded_url media)})))

(defn create-hashtags-from-tweet [twitter-tweet]
  (let [hts-attrs (build-hashtags twitter-tweet)]
    (if (not (empty? hts-attrs))
      (naan/create hashtags hts-attrs)
      :no-hashtags)))

(defn find-or-create-url [url]
  "Looks for a url matching the [short] url in the supplied url entity map.
   If found, returns that url. If no url exists, one is created and
   the new url is returned."
  (let [extended-url (:extended_url url)
        existing-url-ent (naan/read urls {:extended_url extended-url})]
    (if existing-url-ent
      existing-url-ent
      (naan/create urls url))))

(defn create-urls-from-tweet [twitter-tweet]
  (doseq [url (concat (build-urls twitter-tweet) (build-media twitter-tweet))]
    (let [url-id (:id (find-or-create-url url))
          tweet-id (:id twitter-tweet)]
      (naan/create url-in-tweets {:tweet_id tweet-id :url_id url-id}))))

(defn create-sentiment-for-tweet [tweet-id]
  (naan/create tweet-sentiment {:tweet_id tweet-id}))

(defn find-or-create-author-from-tweet [twitter-tweet]
  (let [author_id (:id (:user twitter-tweet))]
    (if-let [author-ent (naan/read tweet-authors author_id)]
        author-ent
        (naan/create tweet-authors (build-tweet-author twitter-tweet)))))

(defn create-tweet-entities [twitter-tweet]
  "Creates a tweet entry in the database along with all associated entities.
   Wraps in a transaction to ensure this happens atomically."
  (let [tweet-id (:id twitter-tweet)]
    (transaction
      (if (retweet? twitter-tweet)
        ; always create parent first
        (create-tweet-entities (get-parent twitter-tweet)))
      (if (naan/read tweets tweet-id)
        (log/info (str "HIT existing tweet: " tweet-id))
        (do
          (log/info (str "CREATE tweet: " tweet-id))
          (naan/create tweets (twitter-tweet-to-db-tweet twitter-tweet))
          (if (not (retweet? twitter-tweet))
              ; create a sentiment for this tweet if it isn't a retweet
              (create-sentiment-for-tweet tweet-id))
          (create-hashtags-from-tweet twitter-tweet)
          (create-urls-from-tweet twitter-tweet)
          (find-or-create-author-from-tweet twitter-tweet))))))
