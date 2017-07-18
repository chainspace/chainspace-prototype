from flask import Flask
from flask import jsonify
from flask import request


class ChainspaceContract(object):
    def __init__(self, contract_name):
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
        self.dependencies.append(contract)
        contract.register_callback(self.local_callback)

    def run(self):
        self.run_checker_service()

    def run_checker_service(self):
        self._populate_empty_checkers()
        self.flask_app.run()

    def checker(self, method_name):
        def checker_decorator(function):
            self.checkers[method_name] = function

            def function_wrapper(*args, **kwargs):
                return jsonify({'success': function(*args, **kwargs)})

            @self.flask_app.route('/' + self.contract_name + '/' + method_name, methods=['POST'], endpoint=method_name)
            def checker_request():
                dependencies = (request.json['dependencies'] if 'dependencies' in request.json else [])
                for dependency in dependencies:
                    dependency['inputs'] = tuple(dependency['inputs'])
                    dependency['reference_inputs'] = tuple(dependency['reference_inputs'])
                    dependency['outputs'] = tuple(dependency['outputs'])

                return function_wrapper(
                    tuple(request.json['inputs']),
                    tuple(request.json['reference_inputs']),
                    request.json['parameters'],
                    tuple(request.json['outputs']),
                    request.json['returns'],
                    dependencies
                )

            return function_wrapper

        return checker_decorator

    def method(self, method_name):
        def method_decorator(function):
            def function_wrapper(inputs=None, reference_inputs=None, parameters=None, *args, **kwargs):
                if '__checker_mode' in kwargs:
                    if kwargs['__checker_mode']:
                        _checker_mode.on = True
                    del kwargs['__checker_mode']
                checker_mode = _checker_mode.on

                if inputs is None:
                    inputs = ()
                if reference_inputs is None:
                    reference_inputs = ()
                if parameters is None:
                    parameters = {}

                self.dependent_transactions_log = []
                if self.methods_original['init'] == function:
                    result = function()
                else:
                    result = function(inputs, reference_inputs, parameters, *args, **kwargs)

                for key in ('outputs', 'returns', 'extra_parameters'):
                    if key not in result or key is None:
                        result[key] = ({} if key == 'returns' else tuple())

                result['parameters'] = parameters
                result['parameters'].update(result['extra_parameters'])
                del result['extra_parameters']

                if checker_mode:
                    result['inputs'] = inputs
                    result['reference_inputs'] = reference_inputs
                else:
                    store = {}
                    for obj in inputs + reference_inputs:
                        store[obj.object_id] = obj

                    result['inputIDs'] = [obj.object_id for obj in inputs]
                    result['referenceInputIDs'] = [obj.object_id for obj in reference_inputs]

                result['contractID'] = self.contract_name

                result['dependencies'] = self.dependent_transactions_log

                outputs = []
                for obj in result['outputs']:
                    outputs.append(str(obj))
                result['outputs'] = tuple(outputs)

                if checker_mode:
                    dependencies = []
                    for dependency in result['dependencies']:
                        dependencies.append(dependency['solution'])
                    result['dependencies'] = dependencies
                    return_value = {'solution': result}
                else:
                    dependencies = []
                    for dependency in result['dependencies']:
                        store.update(dependency['store'])
                        dependencies.append(dependency['transaction'])
                    result['dependencies'] = dependencies
                    return_value = {'transaction': result, 'store': store}

                    outputs = []
                    for output_index in range(len(result['outputs'])):
                        outputs.append(ChainspaceObject.from_transaction(result, output_index))
                    result['outputs'] = tuple(outputs)

                self._trigger_callbacks(return_value)
                _checker_mode.on = False
                return return_value

            self.methods[method_name] = function_wrapper
            self.methods_original[method_name] = function

            return function_wrapper

        return method_decorator


    def register_standard_checker(self, method_name, function):
        @self.checker(method_name)
        def checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
            result = function(inputs, reference_inputs, parameters, __checker_mode=True)
            solution = result['solution']

            for dependency in solution['dependencies']:
                del dependency['dependencies']

            return (
                solution['outputs'] == outputs
                and solution['returns'] == returns
                and solution['dependencies'] == dependencies
            )


class ChainspaceObject(str):
    def __new__(cls, object_id, value):
        return super(ChainspaceObject, cls).__new__(cls, value)

    def __init__(self, object_id, value):
        self.object_id = object_id

    @staticmethod
    def from_transaction(transaction, output_index):
        """
        Return a ChainspaceObject from a transaction output.

        transaction: the transaction.
        output_index: the index of the output.
        """
        obj = transaction['outputs'][output_index]
        object_id = 0 # TODO: calculate the actual object ID.
        return ChainspaceObject(object_id, obj)


class _CheckerMode(object):
    def __init__(self):
        self.on = False

_checker_mode = _CheckerMode()
