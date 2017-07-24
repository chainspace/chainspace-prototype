import requests

class ChainspaceClient(object):
    def __init__(self, host='127.0.0.1', port=3001):
        self.host = host
        self.port = port

    @property
    def url(self):
        return 'http://{}:{}'.format(self.host, self.port)

    def push_transaction(self, transaction):
        requests.post(self.url, transaction)
