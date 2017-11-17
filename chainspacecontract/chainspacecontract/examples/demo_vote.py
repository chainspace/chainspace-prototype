""" Script that does some voting using the vote.py contract """

from json            import dumps, loads
from chainspacecontract import transaction_to_solution
from chainspacecontract.examples.vote import contract as vote_contract
from chainspacecontract.examples import vote

from chainspaceapi import ChainspaceClient

# crypto
from chainspacecontract.examples.utils import setup, key_gen, pack

client = ChainspaceClient()


tx_init = vote.init()

client.process_transaction(tx_init)


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
    (token, ),
    None,
    None,
    dumps(options),
    dumps(participants),
    pack(tally_priv),
    pack(tally_pub)
)
