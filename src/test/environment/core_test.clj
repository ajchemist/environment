(ns environment.core-test
  (:require
   [clojure.test :as test :refer [deftest is are testing]]
   [environment.core :as env]
   ))


(deftest main
  (is (nil? (env/config-edn-file-or-resource-source "config/test-n.edn")))
  (is (nil? (env/slurp-edn-map (env/config-edn-file-or-resource-source "config/test-n.edn") nil)))
  (is (map? (env/slurp-edn-map (env/config-edn-file-or-resource-source "config/test.edn") nil)))

  (is (==
        (:x
         (env/merge-system-maps
           {}
           nil
           [(env/config-edn-file-or-resource-source "config/test.edn")
            (env/config-edn-file-or-resource-source "config/test-2.edn")]))
        1))


  (is
    (==
      (:x
       (env/merge-system-maps-2
         {}
         nil
         [(env/config-edn-file-or-resource-source "config/test.edn")
          (env/config-edn-file-or-resource-source "config/test-2.edn")]))
      2))
  )
