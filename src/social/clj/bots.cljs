(ns social.clj.bots
  (:require ["masto$login" :as login]
            ["redis$default" :as redis]
            ["feed-reader$read" :as feed]
            ["yaml$default" :as yaml]
            ["fs" :as fs]
            [nbb.core :as nbb]
            [clojure.walk :as walk]
            [promesa.core :as p]))

(defn masto [token]
  "instantiate mastodon client, using token from env var"
  (login #js{:url "https://clj.social"
             :accessToken token}))

(def redis-conn
  "redis client init"
  (redis/createClient
   #js{:url js/process.env.DATABASE_URL}))

(defn toot [status visibility key token]
  "public toot"
  (nbb/await
   (p/let [cli (masto token)
           body (.create (.-statuses cli) #js{:status status
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

(defn toot-text [obj hashtags]
  "formats the text that will be published"
  (str (:title obj) "\n\n"
       (:link obj) "\n\n"
       (:description obj) "\n\n"
       hashtags))

(defn feed-reader [clients objs]
  "do in all feed registration and publishing link (key)"
  (let [itens (js->clj objs)
        entries (sort-by :published
                         (walk/keywordize-keys (get itens "entries")))
        client (:client clients)]
    (doseq [obj entries]
      (p/let [get (.get client (:link obj))]
        (if-not get
          (let [body (toot-text obj (clients :hashtags))]
            (.then (toot body "public" (:link obj) (:token clients))
                   (fn [x] (save client x)))))))))

(defn -main []
  (p/let [client redis-conn]
    (p/do
      (.connect client)
      (fn []
        (doseq [[k yml] (get (walk/keywordize-keys
                              (js->clj (yaml/parse
                                        (fs/readFileSync "./bots.yml" "utf8")))) :bots)]
          (let [clients {:client client
                         :token (aget js/process.env (:env yml))
                         :hashtags (:hashtags yml)}]
            (.then (feed (:feed yml))
                   (fn [x] (feed-reader clients x))))))
      (.disconnect client))))
