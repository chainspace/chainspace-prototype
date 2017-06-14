from multiprocessing import Process
import time
import unittest

import requests

from chainspacecontract.contract import ChainspaceContract
from chainspacecontract.examples.increment import contract as increment_contract


class TestIncrement(unittest.TestCase):
    def test_checker_service(self):
        checker_service_process = Process(target=increment_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        response = requests.post('http://127.0.0.1:5000/increment', json={'inputs': [1], 'outputs': [2], 'parameters': {}})
        response_json = response.json()
        self.assertTrue(response_json['success'])

        response = requests.post('http://127.0.0.1:5000/increment', json={'inputs': [1], 'outputs': [3], 'parameters': {}})
        response_json = response.json()
        self.assertFalse(response_json['success'])

        checker_service_process.terminate()

        return response

if __name__ == '__main__':
    unittest.main()
