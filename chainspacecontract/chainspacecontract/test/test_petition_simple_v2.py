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
from chainspacecontract.examples.petition_simple_v2 import contract as petition_simple_contract
from chainspacecontract.examples import petition_simple_v2 as petition_simple


####################################################################
# authenticated bank transfer
####################################################################
class Test(unittest.TestCase):
    # --------------------------------------------------------------
    # test init
    # --------------------------------------------------------------
    def test_init(self):
        ##
        ## run service
        ##
        checker_service_process = Process(target=petition_simple_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        transaction = petition_simple.init()

        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + petition_simple_contract.contract_name + '/init', json=transaction_to_solution(transaction)
        )
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        checker_service_process.terminate()
        checker_service_process.join()


    # --------------------------------------------------------------
    # test create petition
    # --------------------------------------------------------------
    def test_create_petition(self):
        ##
        ## run service
        ##
        checker_service_process = Process(target=petition_simple_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        # set up info and options
        UUID = "1234"
        info = "Here goes what the petition is about."
        options = ['YES', 'NO']

        # init
        init_transaction = petition_simple.init()
        token = init_transaction['transaction']['outputs'][0]

        # initialise vote (all votes are zero)
        transaction = petition_simple.create_petition(
            (token,),
            None,
            None,
            UUID,
            info,
            dumps(options)
        )

        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + petition_simple_contract.contract_name + '/create_petition', json=transaction_to_solution(transaction)
        )
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        checker_service_process.terminate()
        checker_service_process.join()


    # --------------------------------------------------------------
    # test add a score
    # --------------------------------------------------------------
    def test_add_score(self):
        ##
        ## run service
        ##
        checker_service_process = Process(target=petition_simple_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        # set up info and options
        UUID = "1234"
        info = "Here goes what the petition is about."
        options = ['YES', 'NO']

        # init
        init_transaction = petition_simple.init()
        token = init_transaction['transaction']['outputs'][0]

        # initialise vote (all votes are zero)
        create_petition_transaction = petition_simple.create_petition(
            (token,),
            None,
            None,
            UUID,
            info,
            dumps(options)
        )
        old_petition = create_petition_transaction['transaction']['outputs'][1]

        # add a score
        transaction = petition_simple.add_score(
            (old_petition,),
            None,
            (dumps([1, 0]),)
        )

        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + petition_simple_contract.contract_name + '/add_score', json=transaction_to_solution(transaction)
        )
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        checker_service_process.terminate()
        checker_service_process.join()


    # --------------------------------------------------------------
    # test read result
    # -------------------------------------------------------------- 
    def test_read(self):
        ##
        ## run service
        ##
        checker_service_process = Process(target=petition_simple_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        # set up info and options
        UUID = "1234"
        info = "Here goes what the petition is about."
        options = ['YES', 'NO']

        # init
        init_transaction = petition_simple.init()
        token = init_transaction['transaction']['outputs'][0]

        # initialise vote (all votes are zero)
        create_petition_transaction = petition_simple.create_petition(
            (token,),
            None,
            None,
            UUID,
            info,
            dumps(options)
        )
        empty_petition = create_petition_transaction['transaction']['outputs'][1]

        # add a score 1
        add_score_transaction = petition_simple.add_score(
            (empty_petition,),
            None,
            (dumps([1, 0]),)
        )
        old_petition = add_score_transaction['transaction']['outputs'][0]

        # add a score 2
        add_score_transaction = petition_simple.add_score(
            (old_petition,),
            None,
            (dumps([1, 0]),)
        )
        old_petition = add_score_transaction['transaction']['outputs'][0]

        # add a score 3
        add_score_transaction = petition_simple.add_score(
            (old_petition,),
            None,
            (dumps([0, 1]),)
        )
        petition = add_score_transaction['transaction']['outputs'][0]


        # read result
        transaction = petition_simple.read(
            None,
            (petition,),
            None,
        )

        # print result
        print("\n\n===================== RESULTS =====================\n")
        print(transaction['transaction']['returns'])
        print("\n===================================================\n\n")


        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + petition_simple_contract.contract_name + '/read', json=transaction_to_solution(transaction)
        )
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        checker_service_process.terminate()
        checker_service_process.join()

####################################################################
# main
###################################################################
if __name__ == '__main__':
    unittest.main()
