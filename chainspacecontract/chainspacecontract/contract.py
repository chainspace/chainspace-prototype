from flask import Flask
from flask import jsonify
from flask import request


class ChainspaceContract(object):
    def __init__(self, contract_name):
        self.contract_name = contract_name
        self.flask_app = Flask(contract_name)

        self.methods = {}
        self.checkers = {}

    def _populate_empty_checkers(self):
        for method_name, function in self.methods.iteritems():
            if method_name not in self.checkers:
                self.register_standard_checker(method_name, function)

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
                return function_wrapper(
                    tuple(request.json['inputs']),
                    tuple(request.json['reference_inputs']),
                    request.json['parameters'],
                    tuple(request.json['outputs']),
                    request.json['returns']
                )

            return function_wrapper

        return checker_decorator

    def method(self, method_name):
        def method_decorator(function):
            def function_wrapper(inputs, reference_inputs, parameters, *args, **kwargs):
                if inputs is None:
                    inputs = ()
                if reference_inputs is None:
                    reference_inputs = ()
                if parameters is None:
                    parameters = {}

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

                return result

            self.methods[method_name] = function_wrapper

            return function_wrapper

        return method_decorator


    def register_standard_checker(self, method_name, function):
        @self.checker(method_name)
        def checker(inputs, reference_inputs, parameters, outputs, returns):
            result = function(inputs, reference_inputs, parameters)
            return result['outputs'] == outputs and result['returns'] == returns
