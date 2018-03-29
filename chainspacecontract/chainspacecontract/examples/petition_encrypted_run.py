from multiprocessing import Process
import json
import time
import requests
import pprint

# chainsapce
from chainspacecontract import transaction_to_solution

from chainspacecontract.examples.petition_encrypted import contract as petition_contract
from chainspacecontract.examples import petition_encrypted

# crypto
from chainspacecontract.examples.utils import setup, key_gen, pack


checker_service_process = Process(target=petition_contract.run_checker_service)
checker_service_process.start()

time.sleep(0.1)

pp = pprint.PrettyPrinter(indent=4)


def pp_json(json_str):
    pp.pprint(json.loads(json_str))


def pp_object(obj):
    pp.pprint(obj)


def post_transaction(method, tx):
    requests.post(
        'http://127.0.0.1:5000/' + petition_contract.contract_name + '/' + method,
        json=transaction_to_solution(tx)
    )


params = setup()
(tally_priv, tally_pub) = key_gen(params)
(voter1_priv, voter1_pub) = key_gen(params)
(voter2_priv, voter2_pub) = key_gen(params)
(voter3_priv, voter3_pub) = key_gen(params)

# set up options, participants, and tally's key
options = ['YES', 'NO']
participants = [pack(voter1_pub), pack(voter2_pub), pack(voter3_pub)]


init_transaction = petition_encrypted.init()

# pp_object(init_transaction)

post_transaction("init", init_transaction)

# pp_object(init_transaction)

vote_token = init_transaction['transaction']['outputs'][0]

# Create the vote
tx_create_vote = petition_contract.create_petition((vote_token, ), None, None, json.dumps(options), json.dumps(participants), pack(tally_priv), pack(tally_pub))
post_transaction("create_vote", tx_create_vote)
vote_root = tx_create_vote['transaction']['outputs'][1]


# First voter
tx_add_vote_1 = petition_contract.add_signature((vote_root,), None, None, json.dumps([1, 0]), pack(voter1_priv), pack(voter1_pub))
post_transaction("add_vote", tx_add_vote_1)
vote_1 = tx_add_vote_1['transaction']['outputs'][0]

# Second voter
tx_add_vote_2 = petition_contract.add_signature((vote_1,), None, None, json.dumps([0, 1]), pack(voter2_priv), pack(voter2_pub))
post_transaction("add_vote", tx_add_vote_2)
vote_2 = tx_add_vote_2['transaction']['outputs'][0]

# Third voter
tx_add_vote_3 = petition_contract.add_signature((vote_2,), None, None, json.dumps([1, 0]), pack(voter3_priv), pack(voter3_pub))
post_transaction("add_vote", tx_add_vote_3)
vote_3 = tx_add_vote_3['transaction']['outputs'][0]


# Tally the results
tx_tally = petition_contract.tally((vote_3,), None, None, pack(tally_priv), pack(tally_pub))

post_transaction("tally", tx_tally)

pp_object(tx_tally)


checker_service_process.terminate()
checker_service_process.join()