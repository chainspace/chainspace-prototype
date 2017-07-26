""" Dump contract with a checker returning always True. """

from chainspacecontract import ChainspaceContract

contract = ChainspaceContract('dump')


@contract.method('init')
def init():
    return {
        'outputs': ('dump',)
    }


@contract.method('do_nothing')
def increment(inputs, reference_inputs, parameters):
	return {
	    'outputs': ('dump',)
	}

@contract.checker('do_nothing')
def increment_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    return True


if __name__ == '__main__':
    contract.run()
