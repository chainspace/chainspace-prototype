from multiprocessing import Process
import json
import time
import requests
from datetime import datetime
import pprint

# chainspace

from chainspaceapi import ChainspaceClient
from chainspacecontract import transaction_to_solution

from chainspacecontract.examples.petition_encrypted import contract as petition_contract
from chainspacecontract.examples import petition_encrypted

# crypto
from chainspacecontract.examples.utils import setup, key_gen, pack


pp = pprint.PrettyPrinter(indent=4)

results = []

def pp_json(json_str):
    pp.pprint(json.loads(json_str))


def pp_object(obj):
    pp.pprint(obj)


def post_transaction(tx):
    start_tx = datetime.now()
    response = client.process_transaction(tx)
    client_latency = (datetime.now() - start_tx)
    print response.text
    json_response = json.loads(response.text)
    results.append((json_response['success'], response.url, str(client_latency)))


start_time = datetime.now()

client = ChainspaceClient()

params = setup()
(tally_priv, tally_pub) = key_gen(params)

# set up options, participants, and tally's key
options = ['YES', 'NO']


init_transaction = petition_encrypted.init()

# pp_object(init_transaction)

post_transaction(init_transaction)

# pp_object(init_transaction)

petition_token = init_transaction['transaction']['outputs'][0]

print "\nCreate the petition\n"
tx_create_petition = petition_contract.create_petition((petition_token,), None, None, json.dumps(options), pack(tally_priv), pack(tally_pub))
post_transaction(tx_create_petition)
petition_root = tx_create_petition['transaction']['outputs'][1]


print "\nFirst signature\n"
tx_add_signature_1 = petition_contract.add_signature((petition_root,), None, None, json.dumps([1, 0]))
post_transaction(tx_add_signature_1)
signature_1 = tx_add_signature_1['transaction']['outputs'][0]

print "\nSecond signature\n"
tx_add_signature_2 = petition_contract.add_signature((signature_1,), None, None, json.dumps([0, 1]))
post_transaction(tx_add_signature_2)
signature_2 = tx_add_signature_2['transaction']['outputs'][0]

print "\nThird signature\n"
tx_add_signature_3 = petition_contract.add_signature((signature_2,), None, None, json.dumps([1, 0]))
post_transaction(tx_add_signature_3)
signature_3 = tx_add_signature_3['transaction']['outputs'][0]


# Tally the results
tx_tally = petition_contract.tally((signature_3,), None, None, pack(tally_priv), pack(tally_pub))

post_transaction(tx_tally)

pp_object(tx_tally)

end_time = datetime.now()

print "\n\nSUMMARY:\n"
all_ok = True
for result in results:
    print "RESULT: " + str(result)
    if not (result[0] == 'True'):
        all_ok = False

print "\n\nRESULT OF ALL CONTRACT CALLS: " + str(all_ok) + "\n\n"
print "\n\nTime Taken " + str(datetime.now() - start_time) + "\n\n"