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
# test 1
# try to validate a transaction (call the checker) at an hardcoded address
# -------------------------------------------------------------------------------
def test_request():
    # run the checker
    t = Thread(target=start_checker, args=(app_checker,))
    t.start()

    try:
        # test a valid transfer
        r = requests.post(checker_url, data = dumps(Example_transfer))
        assert loads(r.text)["status"] == "OK"

        # test a transfer with invalid amount
        r = requests.post(checker_url, data = dumps(Example_invalid_transfer))
        assert loads(r.text)["status"] == "Error"

        # test malformed transaction
        r = requests.post(checker_url, data = dumps(Example_malformed_transfer))
        assert loads(r.text)["status"] == "Error"

        # get request
        r = requests.get(checker_url)
        assert loads(r.text)["status"] == "Error"

    finally:
        t._Thread__stop()


# -------------------------------------------------------------------------------
# test 2
# final check: simulate a complete transfer
# -------------------------------------------------------------------------------
def test_transaction():
    # run checker and cspace
    t1 = Thread(target=start_checker, args=(app_checker,))
    t1.start()

    try:
        """
        # add Alice's account to DB
        r = requests.post(r"http://127.0.0.1:3001/api/1.0/debug_load", data = dumps(Sally_account))
        assert loads(r.text)["status"] == "OK"
        ID1 = loads(r.text)["objectID"]

        # add Sally's account to DB
        r = requests.post(r"http://127.0.0.1:3001/api/1.0/debug_load", data = dumps(Alice_account))
        assert loads(r.text)["status"] == "OK"
        ID2 = loads(r.text)["objectID"]

        # get ID of output objects
        # NOTE: hardcoded values; python H(x) does not return the same hash than Java...
        ID3 = "6b88d14940c1294227c2be03fd0affd4fcb8af3a54165d3a51fc1a5d49aaabbd"
        ID4 = "b1405ffcf16294c76367c056610d89ab7cd9da267ee3183cf471e523e91c386b"
        """

        # set transaction
        """
        T = {
            "contractID"        : 10,
            "inputIDs"          : [ID1, ID2],
            "referenceInputIDs" : [],
            "parameters"        : dumps({"amount":8}),
            "outputs"           : [dumps(Sally_account_new), dumps(Alice_account_new)]
        }
        """

        ID1 = "7308c63e4ff99491af54005258e73bccb320edfa2a1a1aab293f051ca63ea64d"
        ID2 = "3cdff76fc23e30ed617d6188170cf111c844b343f4f6ac7d2e0d6794814859e3"
        ID3 = "0701d1f317e8fecd6ac59f71c662e33bd0aaef8a5784ba4a0bfceb5b48c01e41"
        ID4 = "57435d7c4b49b34ec02c425af4ccdc8b92b2841d2956c5a22d86105f08c37f8d"

        T = {
            "contractID"        : 10,
            "inputIDs"          : [ID1, ID2],
            "referenceInputIDs" : [],
            "parameters"        : dumps({"amount":8}),
            "outputIDs"         : [ID3, ID4]
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
        r = requests.post(r"http://127.0.0.1:3001/api/1.0/process_transaction", data = dumps(packet))
        assert loads(r.text)["status"] == "OK"

    finally:
        t1._Thread__stop()



##################################################################################