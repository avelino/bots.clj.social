(ns social.clj.bots
  (:require ["masto$login" :as login]
            ["redis$default" :as redis]
            ["feed-reader$read" :as feed]
            [nbb.core :as nbb]
            [clojure.walk :as walk]
            [promesa.core :as p]))

(def masto
  "instantiate mastodon client, using token from env var"
  (nbb/await
   (login #js{:url "https://clj.social"
              :accessToken js/process.env.ACCESS_TOKEN})))

(def redis-conn
  "redis client init"
  (redis/createClient
   #js{:url js/process.env.DATABASE_URL}))

(defn toot [status visibility key]
  "public toot"
  (nbb/await
   (p/let [body (.create masto.statuses #js{:status status
                                            :visibility visibility})
           obj (js->clj body)
           id (get obj "id")
           createdat (get obj "createdAt")]
     {:toot-createdat createdat
      :toot-id id
      :key key})))

(defn save [client obj]
  "save data in the storer"
  (let [key (:key obj)]
    (prn :save key)
    (nbb/await (.set client key (js/JSON.stringify (clj->js obj))))))

(defn toot-text [obj]
  "formats the text that will be published"
  (str (:title obj) "\n\n"
       (:link obj) "\n\n"
       (:description obj) "\n\n"
       "#clojure #clj #cljs"))

(defn feed-reader [client objs]
  "do in all feed registration and publishing link (key)"
  (let [itens (js->clj objs)
        entries (sort-by :pubDate
                         (walk/keywordize-keys (get itens "entries")))]
    (doseq [obj entries]
      (p/let [get (.get client (:link obj))]
        (if-not get
          (let [body (toot-text obj)]
            ;; (nbb/await (.set client (:key x) (js/JSON.stringify (clj->js x))))
            ;; public
            (.then (toot body "private" (:link obj))
                   (fn [x] (save client x)))))))))

(defn -main []
  (p/let [client redis-conn]
    (.connect client)
    (.then (feed "https://clojure.org/feed.xml")
           (fn [x] (feed-reader client x)))
    ;; (.disconnect client)
    ))
