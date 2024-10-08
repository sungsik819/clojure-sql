(ns pool
  (:require [db :refer [db-spec]]
            [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.connection :as connection])
  (:import (com.zaxxer.hikari HikariDataSource)))

;; connection pool 설정 및 테스트

;; pool을 열어두고 사용하기 -> 10개가 생성 된다.
;; 닫는건 프로그램이 종료되면 자동으로 닫힌다.
(def ds (connection/->pool HikariDataSource db-spec))

(defn data [_]
  ["apple" 0.99 6 100])

;; 10만개가 넘어 가면 sockettimeout이 걸린다.
(defn insert [_]
  (jdbc/execute! ds (sql/format {:insert-into :invoice
                                 :columns [:product :unit_price :unit_count :customer_id]
                                 :values (vec (map data (range 1 100000)))})))

(insert 0)

(defn stress [_]
  (jdbc/execute! ds (sql/format {:select [:*]
                                 :from :invoice
                                 :where [:= :customer_id 100]})))

;; 1개의 session에서는 1개의 pool로만 되는 것 같음 동시에 두개는 안되는 것 같음?
;; thread로 해볼까?
(map stress (range 1000))

(stress 0)

;; 아래 코드는 dataSource를 사용하고 자동으로 pool을 종료 한다.
;; with-open으로 connection pool을 열게 되면 쿼리들을 실행 후 pool도 전부 닫히게 된다.
(with-open [^HikariDataSource ds ds]
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

(with-open [^HikariDataSource ds ds]
  (jdbc/execute! ds (sql/format {:select [:%sleep.5]})))

(jdbc/execute! ds (sql/format {:select [:%sleep.5]}))

(with-open [^HikariDataSource ds (connection/->pool HikariDataSource db-spec)]
  (reduce
   (fn [cost row]
     (+ cost (* (:unit_price row)
                (:unit_count row))))
   0
   (jdbc/plan ds (sql/format {:select [:*]
                              :from :invoice
                              :where [:= :customer_id 100]}))))