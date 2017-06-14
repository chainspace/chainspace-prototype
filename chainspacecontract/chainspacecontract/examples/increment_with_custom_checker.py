"""A simple smart contract which keeps track of an integer that can be
incremented, with a custom checker."""

from chainspacecontract import ChainspaceContract

contract = ChainspaceContract('addition')


@contract.method('init')
def init():
    return 0


@contract.method('increment')
def increment(integer):
    return integer + 1

@contract.checker('increment')
def increment_checker(integer, output):
    return increment(integer) == output

if __name__ == '__main__':
    contract.run()
