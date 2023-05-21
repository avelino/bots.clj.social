(ns social.clj.bots.feed
  (:require ["feed-reader$read" :as feed]
            ["slugify$default" :as slugify]
            ["md5$default" :as md5]
            [social.clj.bots.db :as db]
            [social.clj.bots.mastodon :as mastodon]
            [clojure.walk :as walk]
            [clojure.string :as str]
            [promesa.core :as p]))

(defn unique-hash
  "takes an url, converts it into a slug and returns a hash (md5)"
  [url]
  (md5 (slugify (str/replace url #"https://|http://" ""))))

(defn feed-reader
  "do in all feed registration and publishing link (key)"
  [clients objs]
  (let [itens (js->clj objs)
        entries (sort-by :published
                         (walk/keywordize-keys (get itens "entries")))
        client (:client clients)]
    (doseq [obj entries]
      (p/let [key (unique-hash (:link obj))
              get (.get client key)]
        (if-not get
          (let [body (mastodon/toot-text obj (clients :hashtags))]
            (.then
             (mastodon/toot body "unlisted" key (:token clients))
             (fn [x]
               (try
                 (db/save client x)
                 (catch :default e
                   (mastodon/remove (:toot-id x) (:token clients) e)))))))))))

(defn feed-process
  "download the rss/feed/atom contained in the url"
  [clients url]
  (prn :feed url)
  (p/then (feed url)
          (fn [x] (feed-reader clients x))))
