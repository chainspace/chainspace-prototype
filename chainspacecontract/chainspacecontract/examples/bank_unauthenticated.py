"""A smart contract that implements a simple, unauthenticated bank."""

from chainspacecontract import ChainspaceContract

contract = ChainspaceContract('bank_unauthenticated')


@contract.method('init')
def init():
    return (
        {'name': 'alice', 'balance': 10},
        {'name': 'bob', 'balance': 10},
    )


@contract.method('transfer')
def transfer(from_account, to_account, amount):
    from_account['balance'] -= amount
    to_account['balance'] += amount

    return from_account, to_account

if __name__ == '__main__':
    contract.run()
