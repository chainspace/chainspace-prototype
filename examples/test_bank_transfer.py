##################################################################################
# Chainspace Mock
# test_bank_transfer.py
#
# version: 0.0.1
##################################################################################
from json                   import loads, dumps
from threading              import Thread
from hashlib                import sha256
from binascii               import hexlify
from bank_transfer_checker  import app          as app_checker
import pytest
import requests


##################################################################################
# variables
##################################################################################
# checker URL
checker_url   =  r"http://127.0.0.1:5001/bank/transfer"

# old accounts (before money transfer)
Sally_account = {"accountId": "Sally", "amount": 10}
Alice_account = {"accountId": "Alice", "amount": 0}

# new accounts (after money transfer)
Sally_account_new = {"accountId": "Sally", "amount": 2}
Alice_account_new = {"accountId": "Alice", "amount": 8}

# example transfer 
Example_transfer = {
    "contractMethod"    : checker_url,
    "inputs"            : [Sally_account, Alice_account],
    "referenceInputs"   : [],
    "parameters"        : {"amount":8},
    "outputs"           : [Sally_account_new, Alice_account_new]
}
Example_invalid_transfer = {
    "contractMethod"    : checker_url,
    "inputs"            : [Sally_account, Alice_account],
    "referenceInputs"   : [],
    "parameters"        : {"amount":100},
    "outputs"           : [Sally_account_new, Alice_account_new]
}
Example_malformed_transfer = {
    "contractMethod"    : checker_url,
    # inputs are missing
    "referenceInputs"   : [],
    "parameters"        : {"amount":8},
    "outputs"           : [Sally_account_new, Alice_account_new]
}


##################################################################################
# Hash 
##################################################################################
def H(x):
    return hexlify(sha256(x).digest())


##################################################################################
# run the checker's service
##################################################################################
def start_checker(app):
    try:
        app.run(host="127.0.0.1", port="5001", threaded=True)
    except Exception as e:
        print "The checker is already running."


##################################################################################
# tests
##################################################################################


# -------------------------------------------------------------------------------
# test 2
# final check: simulate a complete transfer
# -------------------------------------------------------------------------------
def test_transaction():
    # run checker and cspace
    t1 = Thread(target=start_checker, args=(app_checker,))
    t1.start()

    try:
        ##
        #IDs
        ##
        ID00 = "1"
        ID01 = "2"
        ID1 = "99e0e2bac064280f66baebe1515fe31ca9fa59c9dca3a7e7ab406a49a8ada0d5"
        ID2 = "826c1cc8ce6b59d78a6655fb7fdaf1dafffad55db4880f3d5a8c30c192f9312c"
        ID3 = "3"
        ID4 = "4"



        #
        #create account
        #
        T = {
            "contractID"        : 5,
            "inputIDs"          : [],
            "referenceInputIDs" : [],
            "parameters"        : dumps({}),
            "returns"           : dumps({}),
            "outputIDs"         : [ID00],
            "dependencies"      : []
        }
        store = [
            {"key": ID00, "value": Sally_account}
        ]
        packet = {"transaction": T, "store": store};
        r = requests.post(r"http://127.0.0.1:3001/api/1.0/transaction/process", data = dumps(packet))
        assert loads(r.text)["status"] == "OK"


        T = {
            "contractID"        : 5,
            "inputIDs"          : [],
            "referenceInputIDs" : [],
            "parameters"        : dumps({}),
            "returns"           : dumps({}),
            "outputIDs"         : [ID01],
            "dependencies"      : []
        }
        store = [
            {"key": ID01, "value": Alice_account}
        ]
        packet = {"transaction": T, "store": store};
        r = requests.post(r"http://127.0.0.1:3001/api/1.0/transaction/process", data = dumps(packet))
        assert loads(r.text)["status"] == "OK"




    
        #
        #make payment
        #
        T = {
            "contractID"        : 10,
            "inputIDs"          : [ID1, ID2],
            "referenceInputIDs" : [],
            "parameters"        : dumps({"amount":8}),
            "returns"           : dumps({}),
            "outputIDs"         : [ID3, ID4],
            "dependencies"      : []
       }


        # set key-value store
        store = [
            {"key": ID1, "value": Sally_account},
            {"key": ID2, "value": Alice_account},
            {"key": ID3, "value": Sally_account_new},
            {"key": ID4, "value": Alice_account_new},
        ]

        # pack transaction
        packet = {"transaction": T, "store": store};

        # sumbit the transaction to the ledger
        r = requests.post(r"http://127.0.0.1:3001/api/1.0/transaction/process", data = dumps(packet))
        assert loads(r.text)["status"] == "OK"
        

    finally:
        t1._Thread__stop()



##################################################################################