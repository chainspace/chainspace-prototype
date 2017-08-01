import requests

class ChainspaceClient(object):
    def __init__(self, host='127.0.0.1', port=5000):
        self.host = host
        self.port = port

    @property
    def url(self):
        return 'http://{}:{}'.format(self.host, self.port)

    def push_transaction(self, transaction):
        endpoint = self.url + '/api/1.0/transaction/process'
        r = requests.post(endpoint, json=transaction)
        return r
