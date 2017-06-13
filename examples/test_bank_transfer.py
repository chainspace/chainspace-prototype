##################################################################################
# Chainspace Mock
# test_bank_transfer.py
#
# version: 0.0.1
##################################################################################
from json                   import loads, dumps
from threading              import Thread
from bank_transfer_checker  import app            as app_checker
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
        # add Alice's account to DB
        r = requests.post(r"http://127.0.0.1:4567/api/1.0/debug_load", data = dumps(Sally_account))
        assert loads(r.text)["status"] == "OK"
        ID1 = loads(r.text)["objectID"]

        # add Sally's account to DB
        r = requests.post(r"http://127.0.0.1:4567/api/1.0/debug_load", data = dumps(Alice_account))
        assert loads(r.text)["status"] == "OK"
        ID2 = loads(r.text)["objectID"]

        # define transfer
        T = {
            "contractID"        : 10,
            "inputIDs"          : [ID1, ID2],
            "referenceInputIDs" : [],
            "parameters"        : dumps({"amount":8}),
            "outputs"           : [dumps(Sally_account_new), dumps(Alice_account_new)]
        }

        # sumbit the transaction to the ledger
        r = requests.post(r"http://127.0.0.1:4567/api/1.0/process_transaction", data = dumps(T))
        assert loads(r.text)["status"] == "OK"

    finally:
        t1._Thread__stop()



##################################################################################