# bots.clj.social

Feed process and public on mastodon (_fediverse_ [clj.social](https://clj.social)):

- clojure.org -> <a rel="me" href="https://clj.social/@clojure">@clojure@clj.social</a>
- planet.clojure.in -> <a rel="me" href="https://clj.social/@planet">@planet@clj.social</a>

> New RSS must be added to the [`bots.yml`](./bots.yml) file

## run (used [nbb](https://github.com/babashka/nbb))

**ENV VARS:**

- `DATABASE_URL`: redis
- `CONFIG_BOTS`: set path `yaml` file __(default `./bots.yml`)__

``` sh
npm i
npm run start
```
