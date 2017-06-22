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
checker_url   = r"http://127.0.0.1:5001/bank/transfer"
node_url      = r"http://127.0.0.1:3001/api/1.0/transaction/process"

# old accounts (before money transfer)
Alice_account    = {"accountId": "Alice", "amount": 0}
Sally_account    = {"accountId": "Sally", "amount": 10}
ID_Alice_account = "826c1cc8ce6b59d78a6655fb7fdaf1dafffad55db4880f3d5a8c30c192f9312c"
ID_Sally_account = "99e0e2bac064280f66baebe1515fe31ca9fa59c9dca3a7e7ab406a49a8ada0d5"


# new accounts (after money transfer)
Alice_account_new = {"accountId": "Alice", "amount": 8}
Sally_account_new = {"accountId": "Sally", "amount": 2}


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
# check a bank tranfer with dependencies
# -------------------------------------------------------------------------------
def test_dependencies():
    # run checker and cspace
    t1 = Thread(target=start_checker, args=(app_checker,))
    t1.start()

    try:

        # -----------------------------------------------------------------------
        # create Alice's account
        # -----------------------------------------------------------------------
        # set transaction
        T1 = {
            "contractID"        : 5,
            "inputIDs"          : [],
            "referenceInputIDs" : [],
            "parameters"        : [],
            "returns"           : [],
            "outputIDs"         : ["20"],
            "dependencies"      : []
        }

        # set store
        store1 = [
            {"key": "20", "value": Alice_account}
        ]

        # assemble
        packet1 = {"transaction": T1, "store": store1};


        # -----------------------------------------------------------------------
        # create Sally's account
        # -----------------------------------------------------------------------
        # set transaction
        T2 = {
            "contractID"        : 5,
            "inputIDs"          : [],
            "referenceInputIDs" : [],
            "parameters"        : [],
            "returns"           : [],
            "outputIDs"         : ["21"],
            "dependencies"      : []
        }

        # set store
        store2 = [
            {"key": "21", "value": Sally_account}
        ]

        # assemble
        packet2 = {"transaction": T2, "store": store2};


        # -----------------------------------------------------------------------
        # make the transfer
        # -----------------------------------------------------------------------
        # set transaction
        T3 = {
            "contractID"        : 10,
            "inputIDs"          : [ID_Sally_account, ID_Alice_account],
            "referenceInputIDs" : [],
            "parameters"        : [dumps({"amount":8})],
            "returns"           : [],
            "outputIDs"         : ["10", "11"],
            "dependencies"      : [dumps(packet1), dumps(packet2)]
        }

        # set store
        store3 = [
            {"key": ID_Sally_account, "value": Sally_account},
            {"key": ID_Alice_account, "value": Alice_account},
            {"key": "10", "value": Sally_account_new},
            {"key": "11", "value": Alice_account_new},
        ]

        # assemble
        packet3 = {"transaction": T3, "store": store3};

        # sumbit the transaction to the ledger
        r = requests.post(node_url, data = dumps(packet3))
        assert loads(r.text)["status"] == "OK"
        

    finally:
        t1._Thread__stop()


# -------------------------------------------------------------------------------
# test 3
# check a transaction with dependencies & returns
# -------------------------------------------------------------------------------
def test_dependencies_with_returns():
    # run checker and cspace
    t1 = Thread(target=start_checker, args=(app_checker,))
    t1.start()

    try:

        # -----------------------------------------------------------------------
        # variables
        # -----------------------------------------------------------------------
        tokenX1 = "1"
        tokenX2 = "2"
        tokenX4 = "4"

        ID_tokenX1 = "cde1a2d429ba3e1096b7a004044a655aaef1cbb7829bde8821cadf1ca5c93802"


        # -----------------------------------------------------------------------
        # create a token
        # -----------------------------------------------------------------------
        # set transaction
        T1 = {
            "contractID"        : 100,
            "inputIDs"          : [],
            "referenceInputIDs" : [],
            "parameters"        : [],
            "returns"           : [],
            "outputIDs"         : ["20"],
            "dependencies"      : []
        }

        # set store
        store1 = [
            {"key": "20", "value": tokenX1}
        ]

        # assemble
        packet1 = {"transaction": T1, "store": store1};


        # -----------------------------------------------------------------------
        # read transaction: we use tokenX1 as reference to make a local return
        # -----------------------------------------------------------------------
        # set transaction
        T2 = {
            "contractID"        : 101,
            "inputIDs"          : [],
            "referenceInputIDs" : [ID_tokenX1],
            "parameters"        : [],
            "returns"           : [dumps({"token" : tokenX2})],
            "outputIDs"         : [],
            "dependencies"      : [dumps(packet1)]
        }

        # set store
        store2 = [
            {"key": ID_tokenX1, "value": tokenX1}
        ]

        # assemble
        packet2 = {"transaction": T2, "store": store2};


        # -----------------------------------------------------------------------
        # get the local returns as param and outputs tokenX4
        # -----------------------------------------------------------------------
        # set transaction
        T3 = {
            "contractID"        : 102,
            "inputIDs"          : [],
            "referenceInputIDs" : [],
            "parameters"        : [],
            "returns"           : [],
            "outputIDs"         : ["3"],
            "dependencies"      : [dumps(packet2)]
        }

        # set store
        store3 = [
            {"key": "3", "value": tokenX4},
        ]

        # assemble
        packet3 = {"transaction": T3, "store": store3};

        # sumbit the transaction to the ledger
        r = requests.post(node_url, data = dumps(packet3))
        assert loads(r.text)["status"] == "OK"
        

    finally:
        t1._Thread__stop()



##################################################################################