from flask import Flask
from flask import jsonify
from flask import request


class ChainspaceContract(object):
    def __init__(self, contract_name):
        self.flask_app = Flask(contract_name)

        self.methods = {}
        self.checkers = {}

    def _populate_empty_checkers(self):
        for method_name, function in self.methods.iteritems():
            if method_name not in self.checkers:
                standard_checker = get_standard_checker(function)

                @self.checker(method_name)
                def checker(*args, **kwargs):
                    return standard_checker(*args, **kwargs)

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
                args = request.json['inputs']
                if len(request.json['outputs']) > 1:
                    args.append(tuple(request.json['outputs']))
                else:
                    args.append(request.json['outputs'][0])
                return function_wrapper(*args, **request.json['parameters'])

            return function_wrapper

        return checker_decorator

    def method(self, method_name):
        def method_decorator(function):
            self.methods[method_name] = function

            def function_wrapper(*args, **kwargs):
                return function(*args, **kwargs)

            return function_wrapper

        return method_decorator


def get_standard_checker(function):
    def checker(*args, **kwargs):
        return function(*args[:-1], **kwargs) == args[-1]

    return checker
