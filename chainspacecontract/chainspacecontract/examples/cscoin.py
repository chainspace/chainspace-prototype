""" CSCoin """

####################################################################
# imports
####################################################################
# general
from hashlib import sha256
from json    import dumps, loads
# chainspace
from chainspacecontract import ChainspaceContract
# crypto
from petlib.ecdsa import do_ecdsa_sign, do_ecdsa_verify
from chainspacecontract.examples.utils import setup, key_gen, pack, unpack

## contract name
contract = ChainspaceContract('cscoin')

## dependencies
from chainspacecontract.examples.hello import contract as hello_contract
contract.register_dependency(hello_contract)


####################################################################
# methods
####################################################################
# ------------------------------------------------------------------
# init
# ------------------------------------------------------------------
@contract.method('init')
def init():
    return {
        'outputs': (dumps({'type' : 'BankToken'}),),
    }


# ------------------------------------------------------------------
# create account
# ------------------------------------------------------------------
@contract.method('create_account')
def create_account(inputs, reference_inputs, parameters, pub, callback):

    # new account
    new_account = {
        'type' : 'BankAccount', 
        'pub' : pack(pub), 
        'balance' : 10,
        'callback' : callback
    }

    # return
    return {
        'outputs': (inputs[0], dumps(new_account))
    }

# ------------------------------------------------------------------
# make transfer
# ------------------------------------------------------------------
@contract.method('transfer')
def transfer(inputs, reference_inputs, parameters, *args):
    # compute outputs
    amount = loads(parameters[0])
    new_from_account = loads(inputs[0])
    new_to_account = loads(inputs[1])
    new_from_account["balance"] -= amount
    new_to_account["balance"] += amount

    if loads(inputs[0])['callback'] == None:
        hasher = sha256()
        hasher.update(dumps(inputs).encode('utf8'))
        hasher.update(dumps(parameters[0]).encode('utf8'))
        G = setup()[0]
        priv = args[0]
        sig = do_ecdsa_sign(G, priv, hasher.digest())
    else:
        # create dependency
        # @Mustafa: we need to modify the framework to make possible to pass a callback here;
        # i.e., make possible to execute callback_function(args) for any function passed as argument
        hello_contract.init()
        sig = setup()[0].order().random() # dump

    # return
    return {
        'outputs': (dumps(new_from_account), dumps(new_to_account)),
        'extra_parameters' : (pack(sig),)
    }

# ------------------------------------------------------------------
# read
# ------------------------------------------------------------------
@contract.method('read')
def read(inputs, reference_inputs, parameters):
    return {
        'returns' : (reference_inputs[0],),
    }


####################################################################
# checker
####################################################################
# ------------------------------------------------------------------
# check account's creation
# ------------------------------------------------------------------
@contract.checker('create_account')
def create_account_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:
        account = loads(outputs[1])

        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 2 or len(returns) != 0:
            return False 

        # check types
        if loads(inputs[0])['type'] != 'BankToken' or loads(outputs[0])['type'] != 'BankToken': return False
        if account['type'] != 'BankAccount': return False

        # check fields
        account['pub']
        account['callback']

        # check initial amount
        if account['balance'] != 10: return False

        # otherwise
        return True

    except (KeyError, Exception):
        return False

# ------------------------------------------------------------------
# check transfer
# ------------------------------------------------------------------
@contract.checker('transfer')
def transfer_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:
        amount = loads(parameters[0])
        input_from_account = loads(inputs[0])
        input_to_account = loads(inputs[1])
        output_from_account = loads(outputs[0])
        output_to_account = loads(outputs[1])
        
        # check format
        if len(inputs) != 2 or len(reference_inputs) != 0 or len(outputs) != 2 or len(returns) != 0:
            return False

        if input_from_account['pub'] != output_from_account['pub'] or input_to_account['pub'] != output_to_account['pub']:
            return False

        # check tokens
        if input_from_account['type'] != 'BankAccount' or input_to_account['type'] != 'BankAccount':
            return False
        if output_from_account['type'] != 'BankAccount' or output_to_account['type'] != 'BankAccount':
            return False

        # amount transfered should be non-negative
        if amount < 0:
            return False

        # amount transfered should not exceed balance
        if input_from_account['balance'] < amount:
            return False

        # consistency between inputs and outputs
        if input_from_account['balance'] != output_from_account['balance'] + amount:
            return False
        if input_to_account['balance'] != output_to_account['balance'] - amount:
            return False

        # verify transaction
        if input_from_account['callback'] == None:
            sig = unpack(parameters[1])
            pub = unpack(input_from_account['pub'])
            hasher = sha256()
            hasher.update(dumps(inputs).encode('utf8'))
            hasher.update(dumps(parameters[0]).encode('utf8'))
            G = setup()[0]
            if not do_ecdsa_verify(G, pub, sig, hasher.digest()): return False
        else:
            # verify depend transaction -- specified by 'callback'
            # NOTE: the checker of the dependency is automatcally called
            callback = dependencies[0]
            if callback['contractID']+'.'+callback['methodID'] != input_from_account['callback']: return False
            # NOTE: this is not enough -- this only verifes that a particular process has been called,
            # we also need to verify the inputs of that process: e.g., verifying that a bank transfer has been done
            # is useless if you don't verify the beneficiary. Any idea ?

        # otherwise
        return True

    except (KeyError, Exception):
        return False

# ------------------------------------------------------------------
# check read
# ------------------------------------------------------------------
@contract.checker('read')
def read_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:
        # check format
        if len(inputs) != 0 or len(reference_inputs) != 1 or len(outputs) != 0 or len(returns) != 1:
            return False

        # check values
        if reference_inputs[0] != returns[0]: return False

        # otherwise
        return True

    except (KeyError, Exception):
        return False

####################################################################
# main
####################################################################
if __name__ == '__main__':
    contract.run()



####################################################################
