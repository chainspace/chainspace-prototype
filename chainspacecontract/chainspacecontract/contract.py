from flask import Flask
from flask import jsonify
from flask import request


class ChainspaceContract(object):
    def __init__(self, contract_name):
        self.contract_name = contract_name
        self.flask_app = Flask(contract_name)

        self.methods = {}
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

            @self.flask_app.route('/' + method_name, methods=['POST'], endpoint=method_name)
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
                if inputs is None:
                    inputs = ()
                if reference_inputs is None:
                    reference_inputs = ()
                if parameters is None:
                    parameters = {}

                self.dependent_transactions_log = []
                if function.__name__ == 'init':
                    result = function()
                else:
                    result = function(inputs, reference_inputs, parameters, *args, **kwargs)

                for key in ('outputs', 'returns', 'extra_parameters'):
                    if key not in result or key is None:
                        result[key] = ({} if key == 'returns' else tuple())

                result['parameters'] = parameters
                result['parameters'].update(result['extra_parameters'])
                del result['extra_parameters']

                result['inputs'] = inputs
                result['reference_inputs'] = reference_inputs

                result['contract_id'] = 0

                result['dependencies'] = self.dependent_transactions_log

                self._trigger_callbacks(result)
                return result

            self.methods[method_name] = function_wrapper

            return function_wrapper

        return method_decorator


    def register_standard_checker(self, method_name, function):
        @self.checker(method_name)
        def checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
            result = function(inputs, reference_inputs, parameters)

            for dependency in result['dependencies']:
                del dependency['dependencies']

            return (
                result['outputs'] == outputs
                and result['returns'] == returns
                and result['dependencies'] == dependencies
            )
