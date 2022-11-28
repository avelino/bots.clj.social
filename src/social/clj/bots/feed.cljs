(ns social.clj.bots.feed
  (:require ["feed-reader$read" :as feed]
            ["slugify$default" :as slugify]
            ["md5$default" :as md5]
            [social.clj.bots.db :as db]
            [social.clj.bots.mastodon :as mastodon]
            [clojure.walk :as walk]
            [clojure.string :as str]
            [promesa.core :as p]))

(defn unique-hash [url]
  "takes an url, converts it into a slug and returns a hash (md5)"
  (md5 (slugify (str/replace url #"https://|http://" ""))))

(defn feed-reader [clients objs]
  "do in all feed registration and publishing link (key)"
  (let [itens (js->clj objs)
        entries (sort-by :published
                         (walk/keywordize-keys (get itens "entries")))
        client (:client clients)]
    (doseq [obj entries]
      (p/let [key (unique-hash (:link obj))
              get (.get client key)]
        (if-not get
          (let [body (mastodon/toot-text obj (clients :hashtags))]
            (.then (mastodon/toot body "public" key (:token clients))
                   (fn [x] (db/save client x)))))))))

(defn feed-process [clients url]
  "download the rss/feed/atom contained in the url"
  (prn :feed url)
  (p/then (feed url)
          (fn [x] (feed-reader clients x))))
