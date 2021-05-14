(ns polylith.clj.core.lib.core
  (:require [polylith.clj.core.util.interface :as util]
            [polylith.clj.core.lib.ns-to-lib :as ns-to-lib]
            [polylith.clj.core.lib.git-size :as git-size]
            [polylith.clj.core.lib.mvn-size :as mvn-size]
            [polylith.clj.core.lib.local-size :as local-size]))

(defn with-size [[name {:keys [mvn/version local/root git/url sha] :as value}] user-home]
  (cond
    version (mvn-size/with-size name version value)
    root (local-size/with-size-and-version name root value)
    url (git-size/with-size-and-version name sha value user-home)))

(defn with-sizes [library-map user-home]
  (util/stringify-and-sort-map (into {} (map #(with-size % user-home) library-map))))

(defn lib-deps [ws-type config top-namespace ns-to-lib namespaces user-home dep-keys]
  (if (= :toolsdeps1 ws-type)
    (ns-to-lib/lib-deps top-namespace ns-to-lib namespaces)
    (with-sizes (get-in config dep-keys) user-home)))

(defn brick-lib-deps [ws-type config top-namespace ns-to-lib namespaces user-home]
  (let [src (lib-deps ws-type config top-namespace ns-to-lib (:src namespaces) user-home [:deps])
        ;; todo: handle leiningen1 correctly.
        test (lib-deps ws-type config top-namespace ns-to-lib (:test namespaces) user-home [:aliases :test :extra-deps])]
    (cond-> {}
            (seq src) (assoc :src src)
            (seq test) (assoc :test test))))
