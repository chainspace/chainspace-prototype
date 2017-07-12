""" test voting system """

####################################################################
# imports
###################################################################
# general
from multiprocessing import Process
import time
import unittest
import requests
# chainsapce
from chainspacecontract.examples.vote import contract as vote_contract
from chainspacecontract.examples import vote
# crypto
from chainspacecontract.examples.utils import setup, key_gen, pack


####################################################################
# voting system
####################################################################
class TestVote(unittest.TestCase):
    # --------------------------------------------------------------
    # test init
    # --------------------------------------------------------------
    def test_init(self):
        """
        ##
        ## run service
        ##
        checker_service_process = Process(target=vote_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        transaction = bank_authenticated.init()

        ##
        ## submit transaction
        ##
        response = requests.post('http://127.0.0.1:5000/' + vote_contract.contract_name + '/init', json=transaction)
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        checker_service_process.terminate()
        checker_service_process.join()
        """


    # --------------------------------------------------------------
    # test create vote event
    # --------------------------------------------------------------
    def test_create_vote(self):
        ##
        ## run service
        ##
        checker_service_process = Process(target=vote_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        # create keys
        params = setup()
        (_, tally_pub)  = key_gen(params)
        (_, voter1_pub) = key_gen(params)
        (_, voter2_pub) = key_gen(params)
        (_, voter3_pub) = key_gen(params)

        # set up options, particpants, and tally's key
        options      = ['alice', 'bob']
        participants = [pack(voter1_pub), pack(voter2_pub), pack(voter3_pub)]
        tally_pub    = pack(tally_pub)

        # create inputs & parameters
        inputs = {'type': 'VoteToken'},

        # pack transaction
        transaction = vote.create_vote(
            inputs,
            None,
            None,
            options,
            participants,
            tally_pub
        )

        ##
        ## submit transaction
        ##
        response = requests.post('http://127.0.0.1:5000/' + vote_contract.contract_name + '/create_vote', json=transaction)
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        checker_service_process.terminate()
        checker_service_process.join()


    # --------------------------------------------------------------
    # test add a vote
    # --------------------------------------------------------------
    def test_add_vote(self):
        ##
        ## run service
        ##
        checker_service_process = Process(target=vote_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        # create keys
        params = setup()
        (_, tally_pub)  = key_gen(params)
        (_, voter1_pub) = key_gen(params)
        (_, voter2_pub) = key_gen(params)
        (_, voter3_pub) = key_gen(params)

        # set up options, particpants, and tally's key
        options      = ['alice', 'bob']
        participants = [pack(voter1_pub), pack(voter2_pub), pack(voter3_pub)]
        tally_pub    = pack(tally_pub)

        # create inputs & parameters
        inputs = {'type': 'VoteToken'},

        # get initial scores
        init_transaction = vote.create_vote(
            inputs,
            None,
            None,
            options,
            participants,
            tally_pub
        )
        init_vote = init_transaction['outputs'][1]

        # pack transaction
        transaction = vote.add_vote(
            init_vote,
            None,
            None,
            voter1_pub,
        )

        ##
        ## submit transaction
        ##
        response = requests.post('http://127.0.0.1:5000/' + vote_contract.contract_name + '/add_vote', json=transaction)
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
