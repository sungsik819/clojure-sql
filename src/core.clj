(ns core
  (:require [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [next.jdbc.connection :as connection])
  (:import (com.zaxxer.hikari HikariDataSource)))

(def jdbc-url (connection/jdbc-url {:dbtype "mysql" :host "127.0.0.1" :dbname "test"}))

(def db-spec {:jdbcUrl jdbc-url
              :username "root" :password "test"
              :dataSourceProperties {:socketTimeout 30}})

;; connection pool 설정 및 테스트
(with-open [^HikariDataSource ds (connection/->pool HikariDataSource db-spec)]
  (.close (jdbc/get-connection ds))
  (jdbc/execute! ds (sql/format {:create-table [:invoice :if-not-exists]
                                 :with-columns
                                 [[:id :int :auto-increment :primary-key]
                                  [:product [:varchar 32]]
                                  [:unit_price [:decimal 10 2]]
                                  [:unit_count :int :unsigned]
                                  [:customer_id :int :unsigned]]}))

  (jdbc/execute-one! ds (sql/format {:insert-into :invoice
                                     :columns [:product :unit_price :unit_count :customer_id]
                                     :values [["apple" 0.99 6 100]
                                              ["banana" 1.25 3 100]
                                              ["cucumber" 2.49 3 100]]})))

(with-open [^HikariDataSource ds (connection/->pool HikariDataSource db-spec)]
  (reduce
   (fn [cost row]
     (+ cost (* (:unit_price row)
                (:unit_count row))))
   0
   (jdbc/plan ds (sql/format {:select [:*]
                              :from :invoice
                              :where [:= :customer_id 100]}))))

;; sample로 이것 저것 테스트중
(def ^:private db {:dbtype "h2:mem" :dbname "example"})

(def ^:private ds (jdbc/get-datasource db))

(def ^:private ds-opts (jdbc/with-options ds
                         {:return-keys true
                          :builder-fn rs/as-unqualified-lower-maps}))

(def execute! (comp (partial jdbc/execute! ds-opts)
                    sql/format))

(def execute-one! (comp (partial jdbc/execute-one! ds-opts)
                        sql/format))

(def plan (comp (partial jdbc/plan ds-opts)
                sql/format))

(sql/format {:create-table [:address :if-not-exists]
             :with-columns
             [[:id :int :auto-increment :primary-key]
              [:name [:varchar 32]]
              [:email [:varchar 255]]]})

(jdbc/execute-one! ds ["
insert into invoice (product, unit_price, unit_count, customer_id)
values ('apple', 0.99, 6, 100),
       ('banana', 1.25, 3, 100),
       ('cucumber', 2.49, 2, 100)
"])

;; 실행 용도
(jdbc/execute! ds ["select * from invoice where customer_id = ?" 100])

;; plan은 함수와 연산하기 위한 용도
(reduce
 (fn [cost row]
   (+ cost (* (:unit_price row)
              (:unit_count row))))
 0
 (plan {:select [:*]
        :from :invoice
        :where [:= :customer_id 100]}))


(execute! {:create-table [:address :if-not-exists]
           :with-columns
           [[:id :int :auto-increment :primary-key]
            [:name [:varchar 32]]
            [:email [:varchar 255]]]})

(defn add-address [name email]
  {:insert-into :address
   :columns [:name :email]
   :values [[name email]]})


(execute! (add-address "test" "test@test.com"))

(execute! {:select [:*] :from [:address]})

(execute-one! {:select [:id :name :email]
               :from [:address]
               :where [:= :id 1]})