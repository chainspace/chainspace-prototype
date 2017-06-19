"""A smart contract that implements a simple, unauthenticated bank."""

from chainspacecontract import ChainspaceContract

contract = ChainspaceContract('bank_unauthenticated')


@contract.method('init')
def init():
    return {
        'outputs': (
            {'name': 'alice', 'balance': 10},
            {'name': 'bob', 'balance': 10}
        )
    }


@contract.method('transfer')
def transfer(inputs, reference_inputs, parameters):
    from_account = inputs[0]
    to_account = inputs[1]

    from_account['balance'] -= parameters['amount']
    to_account['balance'] += parameters['amount']

    return {
        'outputs': (from_account, to_account)
    }

if __name__ == '__main__':
    contract.run()
