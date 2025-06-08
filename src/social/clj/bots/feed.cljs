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

(defn link-exists?
  "simplified version - always returns public to avoid network calls that may cause errors"
  [link]
  (println :link-visibility link "public")
  "public")

(defn matcher?
  "checks if the title or description contains the matcher"
  [matcher obj]
  (if (nil?  matcher)
    true
    (if (nil? (.match (str (:title obj) (:description obj)) matcher))
      false
      true)))

(defn feed-reader
  "do in all feed registration and publishing link (key)"
  [clients objs]
  (let [itens (js->clj objs)
        entries (sort-by :published
                         (walk/keywordize-keys (get itens "entries")))
        client (:client clients)
        filtered-entries (take-while #(matcher? (:matcher clients) %) entries)]
    (println "Processing" (count filtered-entries) "feed entries")
    (p/all (for [obj filtered-entries]
             (p/catch
              (p/let [key (unique-hash (:link obj))
                      get (.get client key)]
                (println "Checking entry:" (:title obj) "key:" key)
                ;; if the key is not present in the db
                (if-not get
                  ;; publishing levels: public, unlisted
                  (p/let [body (mastodon/toot-text obj (clients :hashtags))
                          visibility (link-exists? (:link obj))
                          toot (mastodon/toot body visibility key (:token clients))]
                    (try
                      (println "Saving new toot for key:" key)
                      (db/save client toot)
                      toot
                      (catch :default e
                        (println "Error saving toot, removing from mastodon:" e)
                        ;; if the toot is not saved, remove it from mastodon
                        (mastodon/remove (:toot-id toot)
                                         (:token clients) e))))
                  (do
                    (println "Skipping existing entry:" key)
                    ;; return nil if already processed
                    nil)))
              (fn [error]
                (println "Error processing entry:" (:title obj) "error:" error)
                nil))))))

(defn feed-process
  "download the rss/feed/atom contained in the url"
  [clients url & {:keys [xml] :or {xml false}}]
  (println "Processing feed:" url "xml mode:" xml)
  (p/catch
   (let [headers #js{:headers #js{:user-agent "Mozilla/5.0"
                                  :content-type "application/rss+xml"}}]
     (if-not xml
       (p/let [feed-data (extract url headers)]
         (println "Feed extracted successfully from:" url)
         (feed-reader clients feed-data))
       (p/let [req (fetch url headers)
               body (.text req)
               feed-data (extract-xml body)]
         (println "XML feed processed successfully from:" url)
         (feed-reader clients feed-data))))
   (fn [error]
     (println "Error processing feed:" url "error:" error)
     (p/resolved []))))
