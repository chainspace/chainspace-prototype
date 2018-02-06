import hashlib
import json
from copy import deepcopy
from contextlib import contextmanager


import click
from flask import Flask
from flask import jsonify
from flask import request

class ChainspaceContract(object):
    def __init__(self, contract_name):
        """ Initializes a new contract by name. """

        self.contract_name = contract_name
        self.flask_app = Flask(contract_name)

        self.methods = {}
        self.methods_original = {}
        self.checkers = {}
        self.callbacks = []
        self.dependencies = []
        self.dependent_transactions_log = []

    def __getattr__(self, key):
        return self.methods[key]

    def _populate_empty_checkers(self):
        for method_name, function in self.methods.iteritems():
            if method_name not in self.checkers:
                self.register_standard_checker(method_name, function)

    def _trigger_callbacks(self, transaction):
        for callback_function in self.callbacks:
            callback_function(transaction)

    def register_callback(self, callback_function):
        self.callbacks.append(callback_function)

    def local_callback(self, transaction):
        self.dependent_transactions_log.append(transaction)

    def register_dependency(self, contract):
        """ Registers another contract, that can be used as a library for this one. """
        self.dependencies.append(contract)
        contract.register_callback(self.local_callback)

    def run(self):
        """ Runs the checker service (flask application) CLI (default on port 5000). """

        @click.group(help='Chainspace contract: {}'.format(self.contract_name))
        def cli():
            pass

        @cli.command()
        @click.option('-p', '--port', default=5000, help='Port to listen on.', type=int)
        def checker(port):
            """Run checker service."""
            self.run_checker_service(port)

        cli()

    def run_checker_service(self, port=5000):
        """ Runs the flash app on 'port' providing a checker service for the contract. """
        self._populate_empty_checkers()
        self.flask_app.run(port=port)

    @contextmanager
    def test_service(self, port=5000):
        """ A test service context manager that spins up and down a service on a port (default 5000)."""
        
        # We import these here to only burden test code with dependencies
        from multiprocessing import Process
        import time

        # Start web server
        checker_service_process = Process(target=self.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        try:
            yield checker_service_process
    
        finally:
            # Tear down web server
            checker_service_process.terminate()
            checker_service_process.join()

    def checker(self, method_name):
        """ A decorator declaring a function to be a checker for a particular contract procedure.
        The function is expected to have a checker signature of: 

            checker(inputs, reference_inputs, parameters, outputs, returns, dependencies)

        """
        def checker_decorator(function):
            self.checkers[method_name] = function

            def function_wrapper(inputs, reference_inputs, parameters, outputs, returns, dependencies):
                return jsonify({'success': function(inputs, reference_inputs, parameters, outputs, returns, dependencies)})

            @self.flask_app.route('/' + self.contract_name + '/' + method_name, methods=['POST'], endpoint=method_name)
            def checker_request():
                dependencies = (request.json['dependencies'] if 'dependencies' in request.json else ())
                for dependency in dependencies:
                    dependency['inputs'] = tuple(dependency['inputs'])
                    dependency['referenceInputs'] = tuple(dependency['referenceInputs'])
                    dependency['outputs'] = tuple(dependency['outputs'])
                    dependency['parameters'] = tuple(dependency['parameters'])
                    dependency['returns'] = tuple(dependency['returns'])

                return function_wrapper(
                    tuple(request.json['inputs']),
                    tuple(request.json['referenceInputs']),
                    tuple(request.json['parameters']),
                    tuple(request.json['outputs']),
                    tuple(request.json['returns']),
                    dependencies
                )

            return function_wrapper

        return checker_decorator

    def method(self, method_name):
        """ A decordator declaring a function to be a procedure with a certain method_name. 
        The function must have the signature:

            method(inputs, reference_inputs, parameters)
        """
        def method_decorator(function):
            def function_wrapper(inputs=None, reference_inputs=None, parameters=None, *args, **kwargs):
                if '__checker_mode' in kwargs:
                    if kwargs['__checker_mode']:
                        _checker_mode.on = True
                    del kwargs['__checker_mode']
                checker_mode = _checker_mode.on

                # Cleanup defaults.
                inputs = tuple(inputs) if inputs is not None else ()
                reference_inputs = tuple(reference_inputs) if reference_inputs is not None else ()
                parameters = tuple(parameters) if parameters is not None else ()

                self.dependent_transactions_log = []
                if self.methods_original['init'] == function:
                    result = function()
                else:
                    result = function(inputs, reference_inputs, parameters, *args, **kwargs)

                for key in ('outputs', 'returns', 'extra_parameters'):
                    if key not in result or result[key] is None:
                        result[key] = tuple()

                result['parameters'] = parameters + result['extra_parameters']
                del result['extra_parameters']

                if checker_mode:
                    result['inputs'] = inputs
                    result['referenceInputs'] = reference_inputs
                else:
                    store = {}
                    for obj in inputs + reference_inputs:
                        store[obj.object_id] = obj

                    result['inputIDs'] = [obj.object_id for obj in inputs]
                    result['referenceInputIDs'] = [obj.object_id for obj in reference_inputs]

                result['contractID'] = self.contract_name
                result['methodID'] = method_name

                result['dependencies'] = self.dependent_transactions_log

                for obj in result['outputs']:
                    if not isinstance(obj, str):
                        raise ValueError("Outputs objects must be strings.")

                if checker_mode:
                    dependencies = []
                    for dependency in result['dependencies']:
                        dependencies.append(dependency['solution'])
                    result['dependencies'] = dependencies
                    
                    for dependency in result['dependencies']:
                        del dependency['dependencies']
                    return_value = {'solution': result}

                else:
                    dependencies = []
                    for dependency in result['dependencies']:
                        store.update(dependency['store'])
                        dependencies.append(dependency['transaction'])
                    result['dependencies'] = dependencies
                    
                    outputs = []
                    for output_index in range(len(result['outputs'])):
                        outputs.append(ChainspaceObject.from_transaction(result, output_index))
                    result['outputs'] = tuple(outputs)
                    return_value = {'transaction': result, 'store': store}

                self._trigger_callbacks(return_value)

                if __debug__ and not _checker_mode.on:
                    # Automatically run the checker to ensure procedure output passes check.
                    if method_name not in self.checkers and method_name != "init":
                        print("POTENTIAL ERROR: '%s' method has no checker." % method_name)
                    elif method_name != "init":
                        txt = transaction_inline_objects(return_value)

                        app = self.flask_app.test_client()
                        args = app.post('/' + self.contract_name + '/' + method_name, 
                                 data=json.dumps(txt),
                                 content_type='application/json')
                        
                        result = json.loads(args.data)["success"]
                        if not result:
                            print("POTENTIAL ERROR: '%s' method output does not satify checker." % method_name)

                _checker_mode.on = False
                return return_value

            self.methods[method_name] = function_wrapper
            self.methods_original[method_name] = function

            return function_wrapper
        return method_decorator


    def register_standard_checker(self, method_name, function):
        """ A function that registers a simple procedure into a checker. 
        This can only work if the checker simply re-runs the procedure and checks outputs are equal."""

        @self.checker(method_name)
        def checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
            result = function(inputs, reference_inputs, parameters, __checker_mode=True)
            solution = result['solution']

            return (
                solution['outputs'] == outputs
                and solution['returns'] == returns
                and solution['dependencies'] == dependencies
            )


class ChainspaceObject(str):
    """ A string that remembers an object_id. """

    def __new__(cls, object_id, value):
        return super(ChainspaceObject, cls).__new__(cls, value)

    def __init__(self, object_id, value):
        self.object_id = object_id

    def __copy__(self):
        return ChainspaceObject(self.object_id, self)

    def __deepcopy__(self, memo):
        return ChainspaceObject(self.object_id, self)


    @staticmethod
    def from_transaction(transaction, output_index):
        """
        Return a ChainspaceObject from a transaction output.

        transaction: the transaction.
        output_index: the index of the output.
        """
        obj = transaction['outputs'][output_index]
        transaction_json = json.dumps(transaction, sort_keys=True, separators=(',', ':'))
        transaction_digest = hashlib.sha256(transaction_json).hexdigest()
        object_digest = hashlib.sha256(transaction['outputs'][output_index]).hexdigest()
        object_id = '{}|{}|{}'.format(transaction_digest, object_digest, output_index)
        object_id = hashlib.sha256(object_id).hexdigest()
        return ChainspaceObject(object_id, obj)


class _CheckerMode(object):
    def __init__(self):
        self.on = False

_checker_mode = _CheckerMode()

def transaction_inline_objects(data):
    """ Takes a dictionary containing a 'transcation' and a store of object IDs to json objects, 
    and returns a transaction with all object IDs substituted with the actual objects. """
    
    store = deepcopy(data['store'])
    transaction = deepcopy(data['transaction'])

    for dependency in transaction['dependencies']:
        del dependency['dependencies']

    for single_transaction in (transaction,) + tuple(transaction['dependencies']):
        # Transform 'inputs'.
        single_transaction['inputs'] = []
        for object_id in single_transaction['inputIDs']:
            single_transaction['inputs'].append(store[object_id])
        del single_transaction['inputIDs']
        single_transaction['inputs'] = tuple(single_transaction['inputs'])
        
        # Transform 'references'.
        single_transaction['referenceInputs'] = []
        for object_id in single_transaction['referenceInputIDs']:
            single_transaction['referenceInputs'].append(store[object_id])
        del single_transaction['referenceInputIDs']
        single_transaction['referenceInputs'] = tuple(single_transaction['referenceInputs'])

    return transaction

# This is the legacy name. Deprecated.
transaction_to_solution = transaction_inline_objects