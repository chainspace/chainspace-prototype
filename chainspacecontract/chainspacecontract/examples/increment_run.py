from multiprocessing import Process
import json
import time
import requests
import pprint
import uuid

# chainsapce
from chainspacecontract import transaction_to_solution

from chainspacecontract.examples.increment import contract as addition_contract
from chainspacecontract.examples import petition_encrypted

# crypto
from chainspacecontract.examples.utils import setup, key_gen, pack


checker_service_process = Process(target=addition_contract.run_checker_service)
checker_service_process.start()

time.sleep(0.1)

pp = pprint.PrettyPrinter(indent=4)

results = []


def pp_json(json_str):
    pp.pprint(json.loads(json_str))


def pp_object(obj):
    pp.pprint(obj)


def post_transaction(method, tx):
    url = 'http://127.0.0.1:5000/addition/' + method
    response = requests.post(
        url,
        json=transaction_to_solution(tx)
    )
    print response.text
    json_response = json.loads(response.text)
    results.append((json_response['success'], url))


tx_init = addition_contract.init()

# pp_object(tx_init)

post_transaction("init", tx_init)

# pp_object(tx_init)

obj_init = tx_init['transaction']['outputs'][0]

# pp_object(obj_init)
# type(obj_init)
# obj_init.object_id

tx_increment_1 = addition_contract.increment(inputs=[obj_init])
post_transaction("increment", tx_increment_1)
obj_1 = tx_increment_1['transaction']['outputs'][0]


tx_increment_2 = addition_contract.increment(inputs=[obj_1])
post_transaction("increment", tx_increment_2)
obj_2 = tx_increment_2['transaction']['outputs'][0]

tx_increment_3 = addition_contract.increment(inputs=[obj_2])
post_transaction("increment", tx_increment_3)
obj_3 = tx_increment_1['transaction']['outputs'][0]


checker_service_process.terminate()
checker_service_process.join()


print "\n\nSUMMARY:\n"
all_ok = True
for result in results:
    print "RESULT: " + str(result)
    if not result[0]:
        all_ok = False

print "\n\nRESULT OF ALL CONTRACT CALLS: " + str(all_ok) + "\n\n"
