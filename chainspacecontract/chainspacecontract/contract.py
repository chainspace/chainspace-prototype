from flask import Flask
from flask import jsonify
from flask import request


class ChainspaceContract(object):
    def __init__(self, contract_name):
        self.flask_app = Flask(contract_name)

        self.methods = {}
        self.checkers = {}

    def _populate_empty_checkers():
        for method_name, function in self.methods.iteritems():
            if method_name not in self.checkers:
                self.checkers[method_name] = get_standard_checker(function)

    def run():
        self.run_checker_service()

    def run_checker_service():
        self.flask_app.run()

    def checker(self, method_name):
        def checker_decorator(function):
            self.checkers[method_name] = function

            @self.flask_app('/' + method_name, methods=['GET'])
            def checker_request():
                return function(*(request.json['inputs'] + request.json['outputs']))

            def function_wrapper(*args):
                return jsonify({'success': function(*args)})

            return function_wrapper

        return checker_decorator

    def method(self, method_name):
        def method_decorator(function):
            self.methods[method_name] = function

            def function_wrapper(*args):
                return function(*args)

            return function_wrapper

        return method_decorator


def get_standard_checker(function):
    def checker(*args):
        return function(*args[:-1]) == args[-1]

    return checker
