(ns revert.pathom-graphql-test
  (:require
   [clojure.data.json :as json]
   [com.wsscode.pathom3.graphql :as p.gql]
   [com.wsscode.pathom3.connect.built-in.resolvers :as pbir]
   [com.wsscode.pathom3.connect.indexes :as pci]
   [com.wsscode.pathom3.interface.eql :as p.eql]
   [org.httpkit.sni-client :as sni-client]
   [org.httpkit.client :as http]))

(alter-var-root #'org.httpkit.client/*default-client* (fn [_] sni-client/default-client))

;;;; staker
;;
;;
;;

(defn request-subgraph [query url]
  (-> @(http/request
        {:url     url
         :method  :post
         :headers {"Content-Type" "application/json"
                   "Accept"       "*/*"}
         :body    (json/write-str {:query query})})
      (doto tap>)
      :body
      json/read-str))

(comment
  (def env (-> {}
               (pci/register
                [(pbir/equivalence-resolver :staker.Incentive/rewardToken :univ3.Token/id)])
               (p.gql/connect-graphql
                {::p.gql/namespace "univ3"}
                #(request-subgraph % "https://api.thegraph.com/subgraphs/name/ianlapham/uniswap-v3-polygon"))
               (p.gql/connect-graphql
                {::p.gql/namespace "staker"}
                #(request-subgraph % "https://api.thegraph.com/subgraphs/name/revert-finance/uni-v3-vesting-staker-polygon"))))

  ;; WORKS
  (p.eql/process
   env
   {:staker.Incentive/id "0x0bb698b687540a7f44504cf1063a4977d2351f201f18fc50281def0f36349d2e"}
   [:staker.Incentive/contract
    :univ3.Token/symbol])

  (p.eql/process
   env
   [{'(:staker.Query/incentives {:first 10})
     [:staker.Incentive/id
      :staker.Incentive/contract
      :univ3.Token/symbol]}])

  )
