from multiprocessing import Process
import json
import time
import requests
import pprint
import uuid

# chainsapce
from chainspacecontract import transaction_to_solution

from chainspacecontract.examples.petition import contract as petition_contract
from chainspacecontract.examples import petition

# crypto
from chainspacecontract.examples.utils import key_gen as pet_keygen
from chainspacecontract.examples.utils import setup as pet_setup
from petlib.ecdsa import do_ecdsa_sign, do_ecdsa_verify
from petlib.bn import Bn

# coconut
from chainspacecontract.examples.coconut_util import pet_pack, pet_unpack, pack, unpackG1, unpackG2
from chainspacecontract.examples.coconut_lib import setup as bp_setup
from chainspacecontract.examples.coconut_lib import ttp_th_keygen, elgamal_keygen, elgamal_dec
from chainspacecontract.examples.coconut_lib import prepare_blind_sign, blind_sign, aggregate_th_sign, randomize
from chainspacecontract.examples.coconut_lib import show_coconut_petition, coconut_petition_verify


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
    url = 'http://127.0.0.1:5000/' + petition_contract.contract_name + '/' + method
    response = requests.post(
        url,
        json=transaction_to_solution(tx)
    )
    print response.text
    json_response = json.loads(response.text)
    results.append((json_response['success'], url))

# crypto parameters
t, n = 4, 5  # threshold and total number of authorities
bp_params = bp_setup()  # bp system's parameters
(sk, vk, vvk) = ttp_th_keygen(bp_params, t, n) # signers keys

# petition parameters
UUID = Bn(1234)
options = ['YES', 'NO']
pet_params = pet_setup()
(priv_owner, pub_owner) = pet_keygen(pet_params)


tx_init = petition.init()

# pp_object(tx_init)

post_transaction("init", tx_init)

# pp_object(init_transaction)

petition_token = tx_init['transaction']['outputs'][0]

print "\nCreate the petition\n"
tx_create_petition = petition_contract.create_petition((petition_token,), None, None, UUID, options, priv_owner, pub_owner, vvk)
post_transaction("create_petition", tx_create_petition)
petition_root = tx_create_petition['transaction']['outputs'][1]
petition_root_list = tx_create_petition['transaction']['outputs'][2]

pp_object(petition_root_list)

# some crypto
# ------------------------------------
(priv_signer, pub_signer) = elgamal_keygen(bp_params)
m = priv_signer
(cm, c, proof_s) = prepare_blind_sign(bp_params, m, pub_signer)
enc_sigs = [blind_sign(bp_params, ski, cm, c, pub_signer, proof_s) for ski in sk]
(h, enc_epsilon) = zip(*enc_sigs)
sigs = [(h[0], elgamal_dec(bp_params, priv_signer, enc)) for enc in enc_epsilon]
sig = aggregate_th_sign(bp_params, sigs)
sig = randomize(bp_params, sig)
(kappa, nu, proof_v) = show_coconut_petition(bp_params, vvk, m, UUID)
# ------------------------------------


print "\nFirst signature\n"
tx_add_signature_1 = petition_contract.sign((petition_root, petition_root_list), None, (json.dumps([1, 0]), ), priv_signer, sig, vvk)
post_transaction("sign", tx_add_signature_1)
signature_1 = tx_add_signature_1['transaction']['outputs'][0]

pp_object(signature_1)

# print "\nSecond signature\n"
# tx_add_signature_2 = petition_contract.add_signature((signature_1,), None, None, json.dumps([0, 1]))
# post_transaction("add_signature", tx_add_signature_2)
# signature_2 = tx_add_signature_2['transaction']['outputs'][0]
#
# print "\nThird signature\n"
# tx_add_signature_3 = petition_contract.add_signature((signature_2,), None, None, json.dumps([1, 0]))
# post_transaction("add_signature", tx_add_signature_3)
# signature_3 = tx_add_signature_3['transaction']['outputs'][0]
#

# Tally the results
# tx_tally = petition_contract.tally((signature_3,), None, None, pack(tally_priv), pack(tally_pub))
#
# post_transaction("tally", tx_tally)
#
# pp_object(tx_tally)

checker_service_process.terminate()
checker_service_process.join()


print "\n\nSUMMARY:\n"
all_ok = True
for result in results:
    print "RESULT: " + str(result)
    if not result[0]:
        all_ok = False

print "\n\nRESULT OF ALL CONTRACT CALLS: " + str(all_ok) + "\n\n"
