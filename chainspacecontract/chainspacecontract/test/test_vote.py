""" test voting system """

####################################################################
# imports
###################################################################
# general
from multiprocessing import Process
from json            import dumps, loads
import time
import unittest
import requests
# chainsapce
from chainspacecontract import transaction_to_solution
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
        ##
        ## run service
        ##
        checker_service_process = Process(target=vote_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        transaction = vote.init()

        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + vote_contract.contract_name + '/init', json=transaction_to_solution(transaction)
        )
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        checker_service_process.terminate()
        checker_service_process.join()


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
        (tally_priv, tally_pub)  = key_gen(params)
        (_, voter1_pub) = key_gen(params)
        (_, voter2_pub) = key_gen(params)
        (_, voter3_pub) = key_gen(params)

        # set up options, particpants, and tally's key
        options      = ['alice', 'bob']
        participants = [pack(voter1_pub), pack(voter2_pub), pack(voter3_pub)]

        # init
        init_transaction = vote.init()
        token = init_transaction['transaction']['outputs'][0]

        # initialise vote (all votes are zero)
        transaction = vote.create_vote(
            (token,),
            None,
            None,
            dumps(options),
            dumps(participants),
            pack(tally_priv),
            pack(tally_pub)
        )

        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + vote_contract.contract_name + '/create_vote', json=transaction_to_solution(transaction)
        )
        print response.json()
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
        (tally_priv, tally_pub)   = key_gen(params)
        (voter1_priv, voter1_pub) = key_gen(params)
        (_, voter2_pub)           = key_gen(params)
        (_, voter3_pub)           = key_gen(params)

        # set up options, particpants, and tally's key
        options      = ['alice', 'bob']
        participants = [pack(voter1_pub), pack(voter2_pub), pack(voter3_pub)]

        # get init token
        init_transaction = vote.init()
        token = init_transaction['transaction']['outputs'][0]

        # initialise vote (all votes are zero)
        create_vote_transaction = vote.create_vote(
            (token,),
            None,
            None,
            dumps(options),
            dumps(participants),
            pack(tally_priv),
            pack(tally_pub)
        )
        old_vote = create_vote_transaction['transaction']['outputs'][1]

        # add a vote
        transaction = vote.add_vote(
            (old_vote,),
            None,
            None,
            dumps([1, 0]),
            pack(voter1_priv),
            pack(voter1_pub)
        )

        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + vote_contract.contract_name + '/add_vote', json=transaction_to_solution(transaction)
        )
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        checker_service_process.terminate()
        checker_service_process.join()


    # --------------------------------------------------------------
    # test add many votes
    # --------------------------------------------------------------
    def test_add_many_votes(self):
        ##
        ## run service
        ##
        checker_service_process = Process(target=vote_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        # number of voters and values
        options = ['alice', 'bob']
        num_voters = 3
        values = [[1, 0] for _ in range(0, num_voters)]

        # create keys and particpants
        params = setup()
        (tally_priv, tally_pub) = key_gen(params)
        keys = [key_gen(params) for _ in range(0, num_voters)]
        participants = [pack(pub) for (_, pub) in keys]

        # get init token
        init_transaction = vote.init()

        # get initial scores
        create_vote_transaction = vote.create_vote(
            (init_transaction['transaction']['outputs'][0],),
            None,
            None,
            dumps(options),
            dumps(participants),
            pack(tally_priv),
            pack(tally_pub)
        )
        vote_0 = create_vote_transaction['transaction']['outputs'][1]

        # add votes
        transaction = {}
        input_obj = vote_0
        for i in range(0, num_voters):
            transaction = vote.add_vote(
                (input_obj,),
                None,
                None,
                dumps(values[i]),        # votes' valu (0 or 1)
                pack(keys[i][0]), # voter's priv key
                pack(keys[i][1])  # voter's pub key
            )
            input_obj = transaction['transaction']['outputs'][0]

            
        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + vote_contract.contract_name + '/add_vote', json=transaction_to_solution(transaction)
        )
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        checker_service_process.terminate()
        checker_service_process.join()

   
    # --------------------------------------------------------------
    # test tally
    # --------------------------------------------------------------
    def test_tally(self):
        ##
        ## run service
        ##
        checker_service_process = Process(target=vote_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        # number of voters and values
        options = ['alice', 'bob']
        num_voters = 3
        values = [[1, 0] for _ in range(0, num_voters)]

        # create keys and particpants
        params = setup()
        (tally_priv, tally_pub) = key_gen(params)
        keys = [key_gen(params) for _ in range(0, num_voters)]
        participants = [pack(pub) for (_, pub) in keys]

        # get init token
        init_transaction = vote.init()

        # get initial scores
        create_vote_transaction = vote.create_vote(
            (init_transaction['transaction']['outputs'][0],),
            None,
            None,
            dumps(options),
            dumps(participants),
            pack(tally_priv),
            pack(tally_pub)
        )
        vote_0 = create_vote_transaction['transaction']['outputs'][1]

        # add votes
        transaction = {}
        input_obj = vote_0
        for i in range(0, num_voters):
            transaction = vote.add_vote(
                (input_obj,),
                None,
                None,
                dumps(values[i]), # votes' valu (0 or 1)
                pack(keys[i][0]), # voter's priv key
                pack(keys[i][1])  # voter's pub key
            )
            input_obj = transaction['transaction']['outputs'][0]

        # tally
        transaction = vote.tally(
            (input_obj,),
            None,
            None,
            pack(tally_priv),
            pack(tally_pub)
        )


        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + vote_contract.contract_name + '/tally', json=transaction_to_solution(transaction)
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
        checker_service_process = Process(target=vote_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        # number of voters and values
        options = ['alice', 'bob', 'sally']
        num_voters = 50
        values = [[1, 0, 0] for _ in range(0, num_voters)]

        # create keys and particpants
        params = setup()
        (tally_priv, tally_pub) = key_gen(params)
        keys = [key_gen(params) for _ in range(0, num_voters)]
        participants = [pack(pub) for (_, pub) in keys]

        # get init token
        init_transaction = vote.init()

        # get initial scores
        create_vote_transaction = vote.create_vote(
            (init_transaction['transaction']['outputs'][0],),
            None,
            None,
            dumps(options),
            dumps(participants),
            pack(tally_priv),
            pack(tally_pub)
        )
        vote_0 = create_vote_transaction['transaction']['outputs'][1]

        # add votes
        transaction = {}
        input_obj = vote_0
        for i in range(0, num_voters):
            transaction = vote.add_vote(
                (input_obj,),
                None,
                None,
                dumps(values[i]), # votes' valu (0 or 1)
                pack(keys[i][0]), # voter's priv key
                pack(keys[i][1])  # voter's pub key
            )
            input_obj = transaction['transaction']['outputs'][0]

        # tally
        transaction = vote.tally(
            (input_obj,),
            None,
            None,
            pack(tally_priv),
            pack(tally_pub)
        )
        result = transaction['transaction']['outputs'][0]

        # read result
        transaction = vote.read(
            None,
            (result,),
            None,
        )

        # print result
        print transaction['transaction']['returns']


        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + vote_contract.contract_name + '/read', json=transaction_to_solution(transaction)
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
