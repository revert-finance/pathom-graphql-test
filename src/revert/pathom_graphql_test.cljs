(ns revert.pathom-graphql-test
  (:require
   [cljs.pprint :as pprint]
   [com.wsscode.pathom3.connect.planner :as pcp]
   [com.wsscode.pathom3.connect.built-in.resolvers :as pbir]
   [com.wsscode.pathom3.connect.indexes :as pci]
   [com.wsscode.pathom3.graphql :as p.gql]
   [com.wsscode.pathom3.interface.async.eql :as p.eql]
   [promesa.core :as p]))

;;;; staker
;;
;;
;;

(defn request-subgraph
  [query url]
  (p/let [headers #js {"Content-Type"  "application/json"
                       "Accept"        "*/*"}
          body (js/JSON.stringify (clj->js {:query query}))
          resp (js/fetch url #js {:method  "POST"
                                  :headers headers
                                  :body    body})
          js-data (.json resp)]
    (js->clj js-data)))

(def env (-> {::pcp/experimental-branch-optimizations true}
             (pci/register
              [(pbir/equivalence-resolver :staker.Incentive/rewardToken :univ3.Token/id)])
             (p.gql/connect-graphql
              {::p.gql/namespace "univ3"}
              #(request-subgraph % "https://api.thegraph.com/subgraphs/name/ianlapham/uniswap-v3-polygon"))
             (p.gql/connect-graphql
              {::p.gql/namespace "staker"}
              #(request-subgraph % "https://api.thegraph.com/subgraphs/name/revert-finance/uni-v3-vesting-staker-polygon"))))

(p/let [env env]
  (tap> env))

(p/let [result (p.eql/process
                env
                [{'(:staker.Query/incentives {:first 10})
                  [:staker.Incentive/id
                   :staker.Incentive/contract
                   :univ3.Token/symbol]}])]
  (pprint/pprint result))

(comment

  (p/let [env env]
    (tap> env))

  ;; WORKS
  (p/let [result (p.eql/process
                  env
                  [{'(:staker.Query/incentives {:first 10})
                    [:staker.Incentive/id
                     :staker.Incentive/contract
                     :univ3.Token/id]}])]
    (pprint/pprint result))

  ;; **WANTED** but FAILS
  (p/let [result (p.eql/process
                  env
                  [{'(:staker.Query/incentives {:first 10})
                    [:staker.Incentive/id
                     :staker.Incentive/contract
                     :univ3.Token/symbol]}])]
    (pprint/pprint result))

  ;; FIXED
  (p/let [result (p.eql/process
                  env
                  {:staker.Incentive/id "0x0bb698b687540a7f44504cf1063a4977d2351f201f18fc50281def0f36349d2e"}
                  [:staker.Incentive/contract
                   :univ3.Token/symbol])]
    (pprint/pprint result))

  ;; FIXED
  (p/let [result (p.eql/process
                  env
                  {:staker.Incentive/id "0x0bb698b687540a7f44504cf1063a4977d2351f201f18fc50281def0f36349d2e"}
                  [:staker.Incentive/contract])]
    (pprint/pprint result)))

(defn init [] (js/console.log "hello"))
