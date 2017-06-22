##################################################################################
# Chainspace Mock
# bank_transfer_checker.py
#
# version: 0.0.1
##################################################################################
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
    # check transfer's format
    ccheck(len(T["referenceInputs"]) == 0, "Expect no references")

    if (T[u"contractID"] == 5):
        return {"status": "OK"}


    # retrieve inputs
    from_account        = loads(T[u"inputs"][0])
    to_account          = loads(T[u"inputs"][1])
    amount              = loads(T[u"parameters"])["amount"]
    from_account_new    = loads(T[u"outputs"][0])
    to_account_new      = loads(T[u"outputs"][1])

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
            return dumps({"status": "ERROR", "message": e.args})
        except Exception as e:
            return dumps({"status": "ERROR", "message": e.args})
    else:
        return dumps({"status": "ERROR", "message":"Use POST method."})


##################################################################################
# execute
##################################################################################
if __name__ == "__main__": 
    app.run(host="127.0.0.1", port="5001") 


##################################################################################