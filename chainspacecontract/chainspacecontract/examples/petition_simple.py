""" 
	A simple smart contract illustarting an insecure e-petition.

	The main problems are:
		1) People can sign the petition multiple time;
		2) No ways to determine who is authorised to sign (access control);
		3) No privacy.
"""


####################################################################
# imports
####################################################################
# general
from json    import dumps, loads
# chainspace
from chainspacecontract import ChainspaceContract

## contract name
contract = ChainspaceContract('petition_simple')


####################################################################
# methods
####################################################################
# ------------------------------------------------------------------
# init
# ------------------------------------------------------------------
@contract.method('init')
def init():
    return {
        'outputs': (dumps({'type' : 'SPToken'}),),
    }

# ------------------------------------------------------------------
# create petition
# NOTE: 
#   - only 'inputs', 'reference_inputs' and 'parameters' are used to the framework
#   - if there are more than 3 param, the checker has to be implemented by hand
# ------------------------------------------------------------------
@contract.method('create_petition')
def create_petition(inputs, reference_inputs, parameters, info, options):
	# inital score
	scores = [0 for _ in loads(options)]

    # new petition object
	new_petition = {
		'type'          : 'SPObject',
		'info'			: info,
		'options'       : loads(options),
		'scores'        : scores
	}

    # return
	return {
		'outputs': (inputs[0], dumps(new_petition)),
	}

# ------------------------------------------------------------------
# add score
# ------------------------------------------------------------------
@contract.method('add_score')
def add_score(inputs, reference_inputs, parameters):

    # retrieve old petition & init new petition object
    old_petition = loads(inputs[0])
    new_petition = loads(inputs[0])
    added_scores = loads(parameters[0])
    
    # update scores
    for i in range(0,len(added_scores)):
        new_petition['scores'][i] = old_petition['scores'][i] + added_scores[i]

    # return
    return {
        'outputs': (dumps(new_petition),),
    }

# ------------------------------------------------------------------
# read
# ------------------------------------------------------------------
@contract.method('read')
def read(inputs, reference_inputs, parameters):

    # return
    return {
        'returns' : (reference_inputs[0],),
    }



####################################################################
# checker
####################################################################
# ------------------------------------------------------------------
# check petition's creation
# ------------------------------------------------------------------
@contract.checker('create_petition')
def create_petition_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:

        # retrieve petition
        petition = loads(outputs[1])
        options = petition['options']
        scores = petition['scores']

        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 2 or len(returns) != 0:
            return False 
        if len(options) < 1 or len(options) != len(scores):
            return False

        # check tokens
        if loads(inputs[0])['type'] != 'SPToken' or loads(outputs[0])['type'] != 'SPToken':
            return False
        if petition['type'] != 'SPObject':
            return False

        # check initalised scores
        if not all(init_score==0 for init_score in scores):
        	return False

        # otherwise
        return True

    except (KeyError, Exception):
        return False

# ------------------------------------------------------------------
# check add score
# ------------------------------------------------------------------
@contract.checker('add_score')
def add_vote_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:

		# retrieve petition
		old_petition = loads(inputs[0])
		new_petition = loads(outputs[0])
		options = new_petition['options']
		scores = new_petition['scores']
		added_scores = loads(parameters[0])

		# check format
		if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 1 or len(returns) != 0:
			return False 
		if len(options) < 1 or len(options) != len(scores):
			return False

		# check tokens
		if new_petition['type'] != 'SPObject':
			return False

		# check that user voted for exactly one option
		if sum(added_scores) != 1:
			return False

		# check scores
		for i in range(0, len(scores)):
			# check scores consistency
			if scores[i] != old_petition['scores'][i] + added_scores[i]:
				return False

			# check that scores are binary
			if added_scores[i] != 0 and added_scores[i] != 1:
				return False


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
        if reference_inputs[0] != returns[0]:
            return False

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