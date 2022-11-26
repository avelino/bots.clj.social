(ns social.clj.bots.feed
  (:require ["feed-reader$read" :as feed]
            [social.clj.bots.db :as db]
            [social.clj.bots.mastodon :as mastodon]
            [clojure.walk :as walk]
            [promesa.core :as p]))

(defn feed-reader [clients objs]
  "do in all feed registration and publishing link (key)"
  (let [itens (js->clj objs)
        entries (sort-by :published
                         (walk/keywordize-keys (get itens "entries")))
        client (:client clients)]
    (doseq [obj entries]
      (p/let [get (.get client (:link obj))]
        (if-not get
          (let [body (mastodon/toot-text obj (clients :hashtags))]
            (.then (mastodon/toot body "public" (:link obj) (:token clients))
                   (fn [x] (db/save client x)))))))))

(defn feed-process [clients url]
  "download the rss/feed/atom contained in the url"
  (prn :feed url)
  (p/then (feed url)
          (fn [x] (feed-reader clients x))))
