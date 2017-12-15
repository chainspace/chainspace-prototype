""" test authenticated bank transfer """

####################################################################
# imports
###################################################################
# general
from multiprocessing import Process
from json            import dumps, loads
import time
import unittest
import requests
# chainspace
from chainspacecontract import transaction_to_solution
from chainspacecontract.examples.coconut import contract as coconut_contract
from chainspacecontract.examples import coconut
# coconut
from chainspacecontract.examples.coconut_util import pack, unpackG1, unpackG2
from chainspacecontract.examples.coconut_lib import setup, elgamal_keygen, mix_ttp_th_keygen
from chainspacecontract.examples.coconut_lib import elgamal_dec, aggregate_th_sign, randomize

# debug
from chainspacecontract.examples.coconut_lib import verify, show_mix_sign, mix_verify, prepare_mix_sign, mix_sign
from bplib.bp import BpGroup, G2Elem

####################################################################
q = 10 # max number of messages
t, n = 2, 3 # threshold and total numbero of authorities
epoch = 1 # coconut's epoch
params = setup(q) # system's parameters
(priv, pub) = elgamal_keygen(params) # user's key pair 
(sk, vk, vvk) = mix_ttp_th_keygen(params, t, n, q) # signers keys


class Test(unittest.TestCase):
    # --------------------------------------------------------------
    # test init
    # --------------------------------------------------------------
    def test_init(self):
        checker_service_process = Process(target=coconut_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ## create transaction
        transaction = coconut.init()

        ## submit transaction
        response = requests.post(
            'http://127.0.0.1:5000/' + coconut_contract.contract_name 
            + '/init', json=transaction_to_solution(transaction)
        )
        self.assertTrue(response.json()['success'])

        ## stop service
        checker_service_process.terminate()
        checker_service_process.join()

    # --------------------------------------------------------------
    # test request issue
    # --------------------------------------------------------------
    def test_request_issue(self):
		## run service
		checker_service_process = Process(target=coconut_contract.run_checker_service)
		checker_service_process.start()
		time.sleep(0.1)

		## create transactions
		# init
		init_transaction = coconut.init()
		token = init_transaction['transaction']['outputs'][0]

		# request issue transaction
		parameters = (q, t, n, epoch)
		G = params[0]
		ID = G.order().random()
		transaction = coconut.request_issue(
		    (token,),
		    None,
		    parameters,
		    pub, 
		    ID
		)

		## submit transaction
		response = requests.post(
		    'http://127.0.0.1:5000/' + coconut_contract.contract_name 
		    + '/request_issue', json=transaction_to_solution(transaction)
		)
		self.assertTrue(response.json()['success'])

		## stop service
		checker_service_process.terminate()
		checker_service_process.join()

    # --------------------------------------------------------------
    # test issue
    # --------------------------------------------------------------
    def test_issue(self):
		## run service
		checker_service_process = Process(target=coconut_contract.run_checker_service)
		checker_service_process.start()
		time.sleep(0.1)

		## create transactions
		# init
		init_transaction = coconut.init()
		token = init_transaction['transaction']['outputs'][0]

		# request issue transaction
		parameters = (q, t, n, epoch)
		G = params[0]
		ID = G.order().random()
		request_issue_transaction = coconut.request_issue(
		    (token,),
		    None,
		    parameters,
		    pub,
		    ID
		)
		issue_request = request_issue_transaction['transaction']['outputs'][1]

		# issue transaction
		transaction = coconut.issue(
		    (issue_request,),
		    None,
		    parameters,
		    sk[0],
		    vvk
		)

		## submit transaction
		response = requests.post(
		    'http://127.0.0.1:5000/' + coconut_contract.contract_name 
		    + '/issue', json=transaction_to_solution(transaction)
		)
		self.assertTrue(response.json()['success'])

		## stop service
		checker_service_process.terminate()
		checker_service_process.join()

    # --------------------------------------------------------------
    # test add
    # --------------------------------------------------------------
    def test_add(self):
		## run service
		checker_service_process = Process(target=coconut_contract.run_checker_service)
		checker_service_process.start()
		time.sleep(0.1)

		## create transactions
		# init
		init_transaction = coconut.init()
		token = init_transaction['transaction']['outputs'][0]

		# request issue transaction
		parameters = (q, t, n, epoch)
		G = params[0]
		ID = G.order().random()
		request_issue_transaction = coconut.request_issue(
		    (token,),
		    None,
		    parameters,
		    pub,
		    ID
		)
		issue_request = request_issue_transaction['transaction']['outputs'][1]

		# issue transaction
		issue_transaction = coconut.issue(
		    (issue_request,),
		    None,
		    parameters,
		    sk[0],
		    vvk
		)
		old_credentials = issue_transaction['transaction']['outputs'][0]
		packed_cm = issue_transaction['transaction']['parameters'][4]
		packed_c = issue_transaction['transaction']['parameters'][5]
		packed_vvk = issue_transaction['transaction']['parameters'][6]

		# add credentials
		transaction = coconut.add(
		    (old_credentials,),
		    None,
		    parameters,
		    sk[0],
		    packed_cm,
		    packed_c,
		    packed_vvk
		)

		## submit transaction
		response = requests.post(
		    'http://127.0.0.1:5000/' + coconut_contract.contract_name 
		    + '/add', json=transaction_to_solution(transaction)
		)
		self.assertTrue(response.json()['success'])

		## stop service
		checker_service_process.terminate()
		checker_service_process.join()

	# --------------------------------------------------------------
    # test spend
    # --------------------------------------------------------------
    def test_spend(self):
		## run service
		checker_service_process = Process(target=coconut_contract.run_checker_service)
		checker_service_process.start()
		time.sleep(0.1)

		## create transactions
		# init
		init_transaction = coconut.init()
		token = init_transaction['transaction']['outputs'][0]
		ID_list = init_transaction['transaction']['outputs'][1]

		# request issue transaction
		parameters = (q, t, n, epoch)
		G = params[0]
		ID = G.order().random()
		request_issue_transaction = coconut.request_issue(
		    (token,),
		    None,
		    parameters,
		    pub,
		    ID
		)
		issue_request = request_issue_transaction['transaction']['outputs'][1]

		# issue transaction
		issue_transaction = coconut.issue(
		    (issue_request,),
		    None,
		    parameters,
		    sk[0],
		    vvk
		)
		old_credentials = issue_transaction['transaction']['outputs'][0]
		packed_cm = issue_transaction['transaction']['parameters'][4]
		packed_c = issue_transaction['transaction']['parameters'][5]
		packed_vvk = issue_transaction['transaction']['parameters'][6]

		# add credentials
		add_transaction = coconut.add(
		    (old_credentials,),
		    None,
		    parameters,
		    sk[1],
		    packed_cm,
		    packed_c,
		    packed_vvk
		)
		credentials = add_transaction['transaction']['outputs'][0]
		enc_sigs = loads(credentials)['sigs']


		# decrypt credentials
		cm = unpackG1(params, packed_cm)
		c = [(unpackG1(params, packed_c[0]), unpackG1(params, packed_c[1]))]
		(h, enc_epsilon1) = mix_sign(params, sk[0], cm, c, [epoch]) 
		(h, enc_epsilon2) = mix_sign(params, sk[1], cm, c, [epoch])

		sigs = []
		for enc_sig in enc_sigs:
			(h, enc_epsilon) = (
				unpackG1(params, enc_sig[0]), 
				(unpackG1(params, enc_sig[1][0]), unpackG1(params, enc_sig[1][1]))
			)
			sigs.append((h, elgamal_dec(params, priv, enc_epsilon)))

		sig = aggregate_th_sign(params, sigs)
		sig = randomize(params, sig)

		# spend credentials
		transaction = coconut.spend(
		    (ID_list,),
		    None,
		    parameters,
		    sig,
		    ID,
		    packed_vvk
		)

		## submit transaction
		response = requests.post(
		    'http://127.0.0.1:5000/' + coconut_contract.contract_name 
		    + '/spend', json=transaction_to_solution(transaction)
		)
		self.assertTrue(response.json()['success'])

		## stop service
		checker_service_process.terminate()
		checker_service_process.join()

####################################################################
# main
###################################################################
if __name__ == '__main__':
    unittest.main()
