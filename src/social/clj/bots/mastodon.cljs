(ns social.clj.bots.mastodon
  (:require ["masto$login" :as login]
            [nbb.core :as nbb]
            [promesa.core :as p]))

(defn masto [token]
  "instantiate mastodon client, using token from env var"
  (login #js{:url "https://clj.social"
             :accessToken token}))

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

(defn remove [toot-id token because]
  "remove toot"
  (prn :toot-remove :toot-id toot-id :because because)
  (nbb/await
   (p/let [cli (masto token)]
     (.remove (.-statuses cli) toot-id))))

(defn toot-text [obj hashtags]
  "formats the text that will be published"
  (str (:title obj) "\n\n"
       (:link obj) "\n\n"
       (:description obj) "\n\n"
       hashtags))
