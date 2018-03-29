from multiprocessing import Process
import json
import time
import requests
import pprint
import uuid

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

results = []

def pp_json(json_str):
    pp.pprint(json.loads(json_str))


def pp_object(obj):
    pp.pprint(obj)


def post_transaction(method, tx):
    response = requests.post(
        'http://127.0.0.1:5000/' + petition_contract.contract_name + '/' + method,
        json=transaction_to_solution(tx)
    )
    results.append(response.status_code)


params = setup()
(tally_priv, tally_pub) = key_gen(params)
signatory_1_id = str(uuid.uuid1())
signatory_2_id = str(uuid.uuid1())
signatory_3_id = str(uuid.uuid1())

# set up options, participants, and tally's key
options = ['YES', 'NO']
participants = [signatory_1_id, signatory_2_id, signatory_3_id]


init_transaction = petition_encrypted.init()

# pp_object(init_transaction)

post_transaction("init", init_transaction)

# pp_object(init_transaction)

petition_token = init_transaction['transaction']['outputs'][0]

print "\nCreate the petition\n"
tx_create_petition = petition_contract.create_petition((petition_token,), None, None, json.dumps(options), json.dumps(participants), pack(tally_priv), pack(tally_pub))
post_transaction("create_petition", tx_create_petition)
petition_root = tx_create_petition['transaction']['outputs'][1]


print "\nFirst signature\n"
tx_add_signature_1 = petition_contract.add_signature((petition_root,), None, None, json.dumps([1, 0]), signatory_1_id)
post_transaction("add_signature", tx_add_signature_1)
signature_1 = tx_add_signature_1['transaction']['outputs'][0]

print "\nSecond signature\n"
tx_add_signature_2 = petition_contract.add_signature((signature_1,), None, None, json.dumps([0, 1]), signatory_2_id)
post_transaction("add_signature", tx_add_signature_2)
signature_2 = tx_add_signature_2['transaction']['outputs'][0]

print "\nThird signature\n"
tx_add_signature_3 = petition_contract.add_signature((signature_2,), None, None, json.dumps([1, 0]), signatory_3_id)
post_transaction("add_signature", tx_add_signature_3)
signature_3 = tx_add_signature_3['transaction']['outputs'][0]


# Tally the results
tx_tally = petition_contract.tally((signature_3,), None, None, pack(tally_priv), pack(tally_pub))

post_transaction("tally", tx_tally)

pp_object(tx_tally)

checker_service_process.terminate()
checker_service_process.join()

all_ok = True
for result in results:
    if result != 200:
        all_ok = False

print "\n\nRESULT OF ALL CHECKER CALLS: " + str(all_ok) + "\n\n"
