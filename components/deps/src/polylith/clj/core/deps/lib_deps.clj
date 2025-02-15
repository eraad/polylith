(ns polylith.clj.core.deps.lib-deps
  (:require [clojure.tools.deps.alpha :as tools-deps]))

(defn name-version [[k {:keys [mvn/version]}]]
  [(str k) version])

(defn adjust-key
  "git deps can have sha or git/sha, tag or git/tag, and deps/root
  These days, git/url can be omitted and deduced from the lib name.

  All deps can have exclusions."
  [{:keys [type path version
           git/url git/sha git/tag deps/root
           exclusions]
    old-sha :sha old-tag :tag}]
  (cond->
   (case type
     "maven" {:mvn/version version}
     "local" {:local/root path}
     "git"   (cond-> {}
               url              (assoc :git/url url)
               (or sha old-sha) (assoc :git/sha (or sha old-sha))
               (or tag old-tag) (assoc :git/tag (or tag old-tag))
               root             (assoc :deps/root root))
     (throw (Exception. (str "Unknown library type: " type))))
   (seq exclusions)
   (assoc :exclusions (vec exclusions))))

(defn key-as-symbol
  "The library names (keys) are stored as strings in the workspace
   and need to be converted back to symbols here.
   Library dependencies are stored as :type and :version and need
   to be translated back to :mvn/version and :local/root."
  [[library version]]
  [(symbol library) (adjust-key version)])

(defn ->config
  "Converts back to the tools.deps format.
   Tools.deps only resolves src depenencies (:deps) but not test
   dependencies (:aliases > :test > :extra-deps) which is the reason
   we merge :src and :test."
  [{:keys [lib-deps maven-repos]}]
  {:mvn/repos maven-repos
   :deps (into {} (map key-as-symbol (merge (:src lib-deps)
                                            (:test lib-deps))))})
(defn resolve-deps [project is-verbose]
  "Resolves which library versions that are used by the given project."
  (let [config (->config project)
        _ (when is-verbose (println (str "# config:\n" config) "\n"))]
    (tools-deps/resolve-deps config {})))
