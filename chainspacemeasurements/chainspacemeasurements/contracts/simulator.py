"""Simulation contract for generating and consuming objects."""
from chainspacecontract import ChainspaceContract

contract = ChainspaceContract('simulator')


@contract.method('init')
def init():
    return {
        'outputs': ('o',)
    }


@contract.method('create')
def create(inputs, reference_inputs, parameters):
    count = int(parameters[0])
    return {
        'outputs': ('o',)*count
    }


@contract.method('consume')
def consume(inputs, reference_inputs, parameters):
    pass
