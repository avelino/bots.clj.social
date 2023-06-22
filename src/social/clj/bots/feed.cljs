(ns social.clj.bots.feed
  (:require ["@extractus/feed-extractor$extract" :as extract]
            ["@extractus/feed-extractor$extractFromXml" :as extract-xml]
            ["cross-fetch$default" :as fetch]
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
        ;; if the key is not present in the db
        (if-not get
          ;; publishing levels: public, unlisted
          (p/let [body (mastodon/toot-text obj (clients :hashtags))
                  toot (mastodon/toot body "public" key (:token clients))]
            (try
              (db/save client toot)
              (catch :default e
                ;; if the toot is not saved, remove it from mastodon
                (mastodon/remove (:toot-id toot)
                                 (:token clients) e)))))))))

(defn feed-process
  "download the rss/feed/atom contained in the url"
  [clients url & {:keys [xml] :or {xml false}}]
  (let [headers #js{:headers #js{:user-agent "Mozilla/5.0"
                                 :content-type "application/rss+xml"}}]
    (if-not xml
      (p/then (extract url headers)
              (fn [x] (feed-reader clients x)))
      (p/let [req (fetch url headers)
              body (.text req)]
        (feed-reader clients (extract-xml body))))))
