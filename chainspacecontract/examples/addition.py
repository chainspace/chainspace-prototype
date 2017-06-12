contract = ChainspaceContract('addition')

@contract.method('init')
def init():
    return 0

@contract.method('increment')
def increment(input_object):
    return input_object + 1

@contract.checker('increment')
def increment_checker(input_object, output_object):
    return increment(input_object) == output_object

if __name__ == '__main__':
    # if --checker is specified, run checker, otherwise run client
    contract.run()
