##################################################################################
# Chainspace Mock
# bank_transfer_checker.py
#
# version: 0.0.1
##################################################################################
from __future__ import print_function # In python 2.7
import sys
from json  import loads, dumps
from flask import Flask, request



##################################################################################
# checker
##################################################################################

# -------------------------------------------------------------------------------
# helper
# -------------------------------------------------------------------------------
def ccheck(V, msg):
    if not V:
        raise Exception(msg)

# -------------------------------------------------------------------------------
# checker
# -------------------------------------------------------------------------------      
def checker_function(T):

    print(T)

    # check transfer's format
    ccheck(T["contractMethod"]       == r"http://127.0.0.1:5001/bank/transfer", "Wrong Method")
    ccheck(len(T["referenceInputs"]) == 0,                                      "Expect no references")

    # retrieve inputs
    from_account, to_account         = T[u"inputs"]
    amount                           = T[u"parameters"]["amount"]
    from_account_new, to_account_new = T[u"outputs"] 

    # check positive amount
    ccheck(0 < amount, "Transfer should be positive")

    # check sender and receiver account
    ccheck(from_account["accountId"] == from_account_new["accountId"],  "Old and new account do not match")
    ccheck(to_account["accountId"]   == to_account_new["accountId"],    "Old and new account do not match")

    # check that the sender has enough fundings
    ccheck(amount <= from_account["amount"], "No funds available")

    # check inntegrity of the operation
    ccheck(from_account["amount"] - amount == from_account_new["amount"], "Incorrect new balance")
    ccheck(to_account["amount"]   + amount == to_account_new["amount"],   "Incorrect new balance")
    
    # return
    return {"status": "OK"}



##################################################################################
# webapp
##################################################################################
# the state of the infrastructure
app = Flask(__name__)

# -------------------------------------------------------------------------------
# /bank/transfer
# checker the correctness of a bank transfer
# -------------------------------------------------------------------------------
@app.route("/bank/transfer", methods=["GET", "POST"])
def check():
    if request.method == "POST":
        try:
            return dumps(checker_function(loads(request.data)))
        except KeyError as e:
            return dumps({"status": "Error", "message": e.args})
        except Exception as e:
            return dumps({"status": "Error", "message": e.args})
    else:
        return dumps({"status": "Error", "message":"Use POST method."})


##################################################################################
# execute
##################################################################################
if __name__ == "__main__": 
    app.run(host="127.0.0.1", port="5001") 


##################################################################################