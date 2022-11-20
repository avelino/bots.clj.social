(ns social.clj.bots
  (:require ["masto$login" :as login]
            ["redis$default" :as redis]
            [nbb.core :as nbb]
            [promesa.core :as p]))

(def masto
  "instantiate mastodon client, using token from env var"
  (nbb/await
   (login #js{:url "https://clj.social"
              :accessToken js/process.env.ACCESS_TOKEN})))

(defn toot [status visibility]
  ;; public
  (nbb/await
   (p/let [body (.create masto.statuses #js{:status status
                                            :visibility visibility})
           obj (js->clj body)
           id (get obj "id")
           createdat (get obj "createdAt")]
     {:createdat createdat :id id})))

(def redis-conn
  "redis client init"
  (redis/createClient
   #js{:url js/process.env.DATABASE_URL}))

(defn save [obj]
  "save data in the storer"
  (p/let [client redis-conn]
    (.connect client)
    (p/let [get (.get client (:id obj))]
      (if-not get
        (nbb/await (.set client (:id obj) (js/JSON.stringify (clj->js obj)))))

      (.disconnect client)

      (prn (:id obj)))))

(defn -main []
  (.then (toot "hello" "private") save))
