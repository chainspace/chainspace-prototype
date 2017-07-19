"""A smart contract that implements a simple, unauthenticated bank."""
import json

from chainspacecontract import ChainspaceContract

contract = ChainspaceContract('bank_unauthenticated')


@contract.method('init')
def init():
    return {
        'outputs': (
            json.dumps({'name': 'alice', 'balance': 10}),
            json.dumps({'name': 'bob', 'balance': 10})
        )
    }


@contract.method('transfer')
def transfer(inputs, reference_inputs, parameters):
    from_account = json.loads(inputs[0])
    to_account = json.loads(inputs[1])
    amount = int(parameters['amount'])

    from_account['balance'] -= amount
    to_account['balance'] += amount

    from_account = json.dumps(from_account)
    to_account = json.dumps(to_account)

    return {
        'outputs': (from_account, to_account)
    }

if __name__ == '__main__':
    contract.run()
