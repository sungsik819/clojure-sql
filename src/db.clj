(ns db
  (:require
   [next.jdbc.connection :as connection]))

(def jdbc-url (connection/jdbc-url {:dbtype "mysql" :host "127.0.0.1" :dbname "test"}))

(def db-spec {:jdbcUrl jdbc-url
              :username "root" :password "test"
              :maximumPoolSize 2})
