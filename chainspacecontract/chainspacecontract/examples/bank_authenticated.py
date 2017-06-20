"""A smart contract that implements a simple, authenticated bank."""

#############################################################################################
# imports
#############################################################################################
from chainspacecontract import ChainspaceContract
from hashlib            import sha256
from petlib.ec          import EcGroup, EcPt
from petlib.ecdsa       import do_ecdsa_sign, do_ecdsa_verify
from petlib.bn          import Bn
from binascii           import hexlify, unhexlify
from json               import dumps

contract = ChainspaceContract('bank_authenticated')


#############################################################################################
# init
#############################################################################################
@contract.method('init')
def init():
    return {
        'outputs': (
            {'name': 'alice', 'balance': 10},
            {'name': 'bob', 'balance': 10}
        )
    }


#############################################################################################
# contract method
#
# NOTE: all extra parameters (like secret_key) will be ingored by the framwork; they will 
#       not be sent anywhere
#############################################################################################
@contract.method('auth_transfer')
def auth_transfer(inputs, reference_inputs, parameters, sk):
    from_account = inputs[0]
    to_account = inputs[1]

    # compute outputs
    from_account['balance'] -= parameters['amount']
    to_account['balance'] += parameters['amount']

    # hash message to sign
    hasher = sha256()
    hasher.update(dumps(inputs).encode('utf8'))
    hasher.update(dumps(reference_inputs).encode('utf8'))
    hasher.update(dumps({'amount' : parameters["amount"]}).encode('utf8'))

    # sign message
    G = EcGroup()
    priv = Bn.from_hex(sk)
    g = G.generator()
    sig = do_ecdsa_sign(G, priv, hasher.digest())

    # return
    return {
        'outputs': (from_account, to_account),
        'extra_parameters' : {
            'signature' : {'r': Bn.hex(sig[0]), 's': Bn.hex(sig[1])}
        }
    }


#############################################################################################
# checker
#############################################################################################
@contract.checker('auth_transfer')
def auth_transfer_checker(inputs, reference_inputs, parameters, outputs, returns):

    # load public key
    G = EcGroup()
    pub = EcPt.from_binary(
        unhexlify('03951d4b7141e99fe1e9d568ef0489db884e37615a6e5968665485a973'), 
        G
    )

    # hash message to verify signature
    hasher = sha256()
    hasher.update(dumps(inputs).encode('utf8'))
    hasher.update(dumps(reference_inputs).encode('utf8'))
    hasher.update(dumps({'amount' : parameters["amount"]}).encode('utf8'))

    # recompose signed digest
    sig = (
        Bn.from_hex(parameters['signature']['r']), 
        Bn.from_hex(parameters['signature']['s'])
    )

    # verify signature
    return do_ecdsa_verify(G, pub, sig, hasher.digest())



#############################################################################################
# main
#############################################################################################
if __name__ == '__main__':
    contract.run()



#############################################################################################
