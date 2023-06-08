(ns social.clj.bots.db
  (:require ["redis$default" :as redis]
            [promesa.core :as p]))

(def redis-conn
  "redis client init"
  (redis/createClient
   #js{:url js/process.env.DATABASE_URL}))

(defn save
  "save data in the storer"
  [client obj]
  (p/let [key (:key obj)]
    (prn :save key)
    (.set client key (js/JSON.stringify (clj->js obj)))))
