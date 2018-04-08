# Network calls in chainspace

## Submit Transaction
Client api sends "TRANSACTION_SUBMIT" to the shard. (MapClient.java send_TRANSACTION_SUBMIT_toShard

This uses AsynchServiceProxy which sends a message to each process in the view asynchronously (on a separate thread)

The latency of this is given by the maximum latency of each node in the shard

ultimately this calls "CommunicationSystemClientSide" implementation to send the message to multiple targets

uses netty channels

attaches the listener you have specified

If the node is the BFTInitiator replica (isBFTInitiator()) - See TreeMapServer:437

TMServer - appExecuteUnordered

It then sends prepare_t synchronously - so this is a network roundtrip

using serviceProxy.invokeOrdered (MapClient:588)

```
[s0n0]  1522826688217 [thread-50] TREEMAP_SERVER ======================================================================================
[s0n0]  1522826688217 [thread-50] TREEMAP_SERVER Incoming request - appExecuteUnordered
[s0n0]  1522826688226 [thread-50] SUBMIT_T (MAIN): --------------------------------------------------------------------

[s0n0]  1522826688226 [thread-50] SUBMIT_T (MAIN): Sending PREPARE_T
[s0n0]  1522826688228 [thread-50] PREPARE_T (DRIVER): >>> Sending PREPARE_T synchronously to shard 0 for transaction f0f110bd77f6b3603c8386b02a866dbc10d26b2ae75c371fe268dc20ee3a57ff
[s0n0]  1522826690651 [thread-50] PREPARE_T (DRIVER): >>> Reply from shard ID 0 is [prepared_t_commit]
[s0n0]  1522826690651 [thread-50] SUBMIT_T (MAIN): Reply to PREPARE_T is prepared_t_commit
[s0n0]  1522826690651 [thread-50] SUBMIT_T (MAIN): --------------------------------------------------------------------

[s0n0]  1522826690651 [thread-50] broadcastBFTDecision (DRIVER): Broadcasting PREPARED_T_COMMIT to shard 0 for transaction f0f110bd77f6b3603c8386b02a866dbc10d26b2ae75c371fe268dc20ee3a57ff


[s0n0]  1522826690653 [thread-50] SUBMIT_T (MAIN): Sending ACCEPT_T_COMMIT
[s0n0]  1522826690653 [thread-50] ACCEPT_T (DRIVER)MapClient is sending accept_t to replicas...
[s0n0]  1522826690654 [thread-50] ACCEPT_T (DRIVER)All shards replied; final response is accepted_t_commit
[s0n0]  1522826690654 [thread-50] SUBMIT_T (MAIN): Reply to ACCEPT_T_COMMIT is accepted_t_commit


[s0n0]  1522826690654 [thread-50] broadcastBFTDecision (DRIVER): Broadcasting ACCEPTED_T_COMMIT to shard 0 for transaction f0f110bd77f6b3603c8386b02a866dbc10d26b2ae75c371fe268dc20ee3a57ff


[s0n0]  1522826690656 [thread-50] SUBMIT_T (MAIN): Finally replying to client: [accepted_t_commit;f0f110bd77f6b3603c8386b02a866dbc10d26b2ae75c371fe268dc20ee3a57ff;<no inputs>]
```


[s0n0]  1522826688246 [thread-22] TREEMAP_SERVER ======================================================================================
[s0n0]  1522826688247 [thread-22] TREEMAP_SERVER Incoming request - appExecuteBatch
[s0n0]  1522826688247 [thread-22] TREEMAP_SERVER executeSingle
[s0n0]  1522826688247 [thread-22] TREEMAP_SERVER executing a request of type PREPARE_T
[s0n0]  1522826688247 [thread-22] PREPARE_T (MAIN) - SINGLE: Received request for transaction f0f110bd77f6b3603c8386b02a866dbc10d26b2ae75c371fe268dc20ee3a57ff

>> PROCESSING TRANSACTION...
addition

>> RUNNING CORE...

Checker instance:

Starting Checker @ http://127.0.0.1:13011/addition/

Working dir: /Users/jmdb/Code/github/DECODEproject/chainspace/chainspacecore-0-0/.
Redirected process output to /Users/jmdb/Code/github/DECODEproject/chainspace/chainspacecore-0-0/./checker.13011.log.0
startChecker: ../.chainspace.env/bin/python contracts/addition.py checker --port 13011
Written pid [84131] to /Users/jmdb/Code/github/DECODEproject/chainspace/chainspacecore-0-0/./checker.pids
checkerProcess isAlive: true
Checker started ok.
        New checker created

Checker URL:
        http://127.0.0.1:13011/addition/init
POST http://127.0.0.1:13011/addition/init HTTP/1.1
{"contractID":"addition","inputs":[],"referenceInputs":[],"parameters":[],"returns":[],"outputs":["0"],"dependencies":[],"methodID":"init"}
HTTP/1.0 200 OK
{
  "success": true
}


The checker accepted the transaction!

New object has been created:
        ID: a9bde7fac83d70a4c6811d74d3cb218abc6c0f69e0dc5a77f0097be61faf79c7
        Object: 0

Transaction added to logs:
        ID: f0f110bd77f6b3603c8386b02a866dbc10d26b2ae75c371fe268dc20ee3a57ff
        Transaction: {"contractID":"addition","dependencies":[],"inputIDs":[],"methodID":"init","outputs":["0"],"parameters":[],"referenceInputIDs":[],"returns":[]}

Updating log's head:
        Old head: NONE
        New head: f0f110bd77f6b3603c8386b02a866dbc10d26b2ae75c371fe268dc20ee3a57ff

>> PRINTING TRANSACTION'S OUTPUT...
[0]
[s0n0]  1522826690646 [thread-22] PREPARE_T (MAIN) - SINGLE: checkPrepare responding with prepared_t_commit

[s0n0]  1522826690652 [thread-26] TREEMAP_SERVER ======================================================================================
[s0n0]  1522826690652 [thread-26] TREEMAP_SERVER Incoming request - appExecuteUnordered
[s0n0]  1522826690652 [thread-26] broadcastBFTDecision (MAIN): Received msg type PREPARED_T_COMMIT

[s0n0]  1522826690654 [thread-26] TREEMAP_SERVER ======================================================================================
[s0n0]  1522826690655 [thread-26] TREEMAP_SERVER Incoming request - appExecuteUnordered
[s0n0]  1522826690655 [thread-26] broadcastBFTDecision (MAIN): Received msg type ACCEPTED_T_COMMIT
[s0n0]  1522826690655 [thread-26] executeAcceptedTCommitCreating outputs for the following transaction:
Inputs:
Outputs:
a9bde7fac83d70a4c6811d74d3cb218abc6c0f69e0dc5a77f0097be61faf79c7

[s0n0]  1522826690655 [thread-26] mapObjectToShardMapped object a9bde7fac83d70a4c6811d74d3cb218abc6c0f69e0dc5a77f0097be61faf79c7 to shard 0. numShards = 2
[s0n0]  1522826690655 [thread-26] CREATE_OBJECT (DRIVER): Mapped object a9bde7fac83d70a4c6811d74d3cb218abc6c0f69e0dc5a77f0097be61faf79c7 to shard 0
[s0n0]  1522826690655 [thread-26] CREATE_OBJECT (DRIVER): Sending CREATE_OBJECT to shard 0
[s0n0]  1522826690656 [thread-26] CREATE_OBJECT (DRIVER): Sent a request to shard ID 0

[s0n0]  1522826690661 [thread-22] TREEMAP_SERVER ======================================================================================
[s0n0]  1522826690661 [thread-22] TREEMAP_SERVER Incoming request - appExecuteBatch
[s0n0]  1522826690661 [thread-22] TREEMAP_SERVER executeSingle
[s0n0]  1522826690661 [thread-22] TREEMAP_SERVER executing a request of type CREATE_OBJECT
[s0n0]  1522826690662 [thread-22] CREATE_OBJECT (MAIN): Created object a9bde7fac83d70a4c6811d74d3cb218abc6c0f69e0dc5a77f0097be61faf79c7
[s0n0]  1522826690662 [thread-29] CREATE_OBJECT (DRIVER): reply received from shard 0 type: ORDERED_REQUEST content: <null>
[s0n0]  1522826690664 [thread-32] CREATE_OBJECT (DRIVER): reply received from shard 0 type: ORDERED_REQUEST content: <null>
[s0n0]  1522826690664 [thread-33] CREATE_OBJECT (DRIVER): reply received from shard 0 type: ORDERED_REQUEST content: <null>
[s0n0]  1522826690670 [thread-31] CREATE_OBJECT (DRIVER): reply received from shard 0 type: ORDERED_REQUEST content: <null>
