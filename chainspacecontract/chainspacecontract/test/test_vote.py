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
            'http://127.0.0.1:5000/' + vote_contract.contract_name + '/init', json=transaction
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
        (_, tally_pub)  = key_gen(params)
        (_, voter1_pub) = key_gen(params)
        (_, voter2_pub) = key_gen(params)
        (_, voter3_pub) = key_gen(params)

        # set up options, particpants, and tally's key
        options      = ['alice', 'bob']
        participants = [pack(voter1_pub), pack(voter2_pub), pack(voter3_pub)]
        tally_pub    = pack(tally_pub)

        # create inputs & parameters
        init_transaction = vote.init()
        token = init_transaction['outputs'][0]

        # initialise vote (all votes are zero)
        transaction = vote.create_vote(
            (token,),
            None,
            None,
            options,
            participants,
            tally_pub
        )

        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + vote_contract.contract_name + '/create_vote', json=transaction
        )
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
        (voter1_priv, voter1_pub) = key_gen(params)
        (_, voter2_pub)           = key_gen(params)
        (_, voter3_pub)           = key_gen(params)

        # set up options, particpants, and tally's key
        options      = ['alice', 'bob']
        participants = [pack(voter1_pub), pack(voter2_pub), pack(voter3_pub)]
        tally_pub    = pack(tally_pub)

        # get init token
        init_transaction = vote.init()
        token = init_transaction['outputs'][0]

        # initialise vote (all votes are zero)
        create_vote_transaction = vote.create_vote(
            (token,),
            None,
            None,
            options,
            participants,
            tally_pub
        )
        old_vote = create_vote_transaction['outputs'][1]

        # add a vote
        transaction = vote.add_vote(
            (old_vote,),
            None,
            None,
            [1, 0],
            pack(voter1_priv),
            pack(voter1_pub)
        )

        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + vote_contract.contract_name + '/add_vote', json=transaction
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
        # create keys
        params = setup()
        (_, tally_pub)  = key_gen(params)
        (voter1_priv, voter1_pub) = key_gen(params)
        (voter2_priv, voter2_pub) = key_gen(params)
        (voter3_priv, voter3_pub) = key_gen(params)

        # set up options, particpants, and tally's key
        options      = ['alice', 'bob']
        participants = [pack(voter1_pub), pack(voter2_pub), pack(voter3_pub)]
        tally_pub    = pack(tally_pub)

        # get init token
        init_transaction = vote.init()

        # get initial scores
        create_vote_transaction = vote.create_vote(
            (init_transaction['outputs'][0],),
            None,
            None,
            options,
            participants,
            tally_pub
        )
        vote_0 = create_vote_transaction['outputs'][1]

        # add vote 1
        add_vote_1_transaction = vote.add_vote(
            (vote_0,),
            None,
            None,
            [1, 0],
            pack(voter1_priv),
            pack(voter1_pub)
        )
        vote_1 = add_vote_1_transaction['outputs'][0]

        # add vote 2
        add_vote_2_transaction = vote.add_vote(
            (vote_1,),
            None,
            None,
            [1, 0],
            pack(voter2_priv),
            pack(voter2_pub)
        )
        vote_2 = add_vote_2_transaction['outputs'][0]

        # add vote 3
        transaction = vote.add_vote(
            (vote_2,),
            None,
            None,
            [1, 0],
            pack(voter3_priv),
            pack(voter3_pub)
        )

        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + vote_contract.contract_name + '/add_vote', json=transaction
        )
        #self.assertTrue(response.json()['success'])

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
        # create keys
        params = setup()
        (tally_priv, tally_pub)   = key_gen(params)
        (voter1_priv, voter1_pub) = key_gen(params)
        (voter2_priv, voter2_pub) = key_gen(params)
        (voter3_priv, voter3_pub) = key_gen(params)

        # set up options, particpants, and tally's key
        options      = ['alice', 'bob']
        participants = [pack(voter1_pub), pack(voter2_pub), pack(voter3_pub)]
        tally_pub    = pack(tally_pub)

        # get init token
        init_transaction = vote.init()

        # get initial scores
        create_vote_transaction = vote.create_vote(
            (init_transaction['outputs'][0],),
            None,
            None,
            options,
            participants,
            tally_pub
        )
        vote_0 = create_vote_transaction['outputs'][1]

        # add vote 1
        add_vote_1_transaction = vote.add_vote(
            (vote_0,),
            None,
            None,
            [1, 0],
            pack(voter1_priv),
            pack(voter1_pub)
        )
        vote_1 = add_vote_1_transaction['outputs'][0]

        # add vote 2
        add_vote_2_transaction = vote.add_vote(
            (vote_1,),
            None,
            None,
            [1, 0],
            pack(voter2_priv),
            pack(voter2_pub)
        )
        vote_2 = add_vote_2_transaction['outputs'][0]

        # add vote 3
        add_vote_3_transaction = vote.add_vote(
            (vote_2,),
            None,
            None,
            [0, 1],
            pack(voter3_priv),
            pack(voter3_pub)
        )
        vote_3 = add_vote_3_transaction['outputs'][0]

        # tally
        transaction = vote.tally(
            (vote_3,),
            None,
            None,
            pack(tally_priv)
        )


        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + vote_contract.contract_name + '/tally', json=transaction
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
