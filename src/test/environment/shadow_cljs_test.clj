(ns environment.shadow-cljs-test
  (:require
   [clojure.test :as test :refer [deftest is are testing]]
   [clojure.string :as str]
   [environment.shadow-cljs :as shadow-cljs]
   ))


(def manifest
  [{:module-id :a :name :a :output-name "a.js"}
   {:module-id :b :name :b :output-name "b.js"}
   {:module-id :a-1 :name :a-1 :output-name "a-1.js" :depends-on #{:a}}
   {:module-id :a-1-1 :name :a-1-1 :output-name "a-1-1.js" :depends-on #{:a-1}}
   {:module-id :b-1 :name :b-1 :output-name "b-1.js" :depends-on #{:b}}
   {:module-id :c :name :c :output-name "c.js" :depends-on #{:b :a-1}}])


(defn- strip-slash-right
  [s]
  (if (str/ends-with? s "/")
    (subs s 0 (dec (count s)))
    s))


(defn- absolutize-path
  [s path]
  (if (str/starts-with? s "/")
    s
    (str (strip-slash-right path) "/" s)))


(deftest main
  (is
    (= (map :module-id (:c (shadow-cljs/transitive-modules manifest)))
       (list :a :b :a-1 :c))))
