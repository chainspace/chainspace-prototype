from flask import Flask
from flask import jsonify
from flask import request


class ChainspaceContract(object):
    def __init__(self, contract_name):
        self.flask_app = Flask(contract_name)

    def run():
        self.flask_app.run()

    def checker(self, method_name):
        def checker_decorator(function):
            @self.flask_app('/' + method_name, methods=['GET'])
            def method_request():
                return function(request.data) # TODO: fix
            def function_wrapper(*args):
                return jsonify({'success': function(*args)})
            return function_wrapper
        return checker_decorator
