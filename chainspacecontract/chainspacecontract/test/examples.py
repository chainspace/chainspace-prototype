from multiprocessing import Process
import time
import unittest

import requests

from chainspacecontract.examples.increment import contract as increment_contract
from chainspacecontract.examples.increment_with_custom_checker import contract as increment_with_custom_checker_contract
from chainspacecontract.examples.bank_unauthenticated import contract as bank_unauthenticated_contract
from chainspacecontract.examples.increment_twice import contract as increment_twice_contract
from chainspacecontract.examples.increment_thrice import contract as increment_thrice_contract


class TestExamples(unittest.TestCase):
    def test_increment_checker_service(self):
        checker_service_process = Process(target=increment_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        response = requests.post('http://127.0.0.1:5000/' + increment_contract.contract_name + '/increment', json={
            'inputs': ['1'],
            'outputs': ['2'],
            'parameters': [],
            'reference_inputs': [],
            'returns': {},
        })
        response_json = response.json()
        self.assertTrue(response_json['success'])

        response = requests.post('http://127.0.0.1:5000/' + increment_contract.contract_name + '/increment', json={
            'inputs': ['1'],
            'outputs': ['3'],
            'parameters': [],
            'reference_inputs': [],
            'returns': {},
        })
        response_json = response.json()
        self.assertFalse(response_json['success'])

        checker_service_process.terminate()
        checker_service_process.join()

    def test_increment_with_custom_checker_service(self):
        checker_service_process = Process(target=increment_with_custom_checker_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        response = requests.post('http://127.0.0.1:5000/' + increment_with_custom_checker_contract.contract_name + '/increment', json={
            'inputs': ['1'],
            'outputs': ['2'],
            'parameters': [],
            'reference_inputs': [],
            'returns': {},
        })
        response_json = response.json()
        self.assertTrue(response_json['success'])

        response = requests.post('http://127.0.0.1:5000/' + increment_with_custom_checker_contract.contract_name + '/increment', json={
            'inputs': ['1'],
            'outputs': ['3'],
            'parameters': [],
            'reference_inputs': [],
            'returns': {},
        })
        response_json = response.json()
        self.assertFalse(response_json['success'])

        checker_service_process.terminate()
        checker_service_process.join()

    def test_bank_unauthenticated_checker_service(self):
        checker_service_process = Process(target=bank_unauthenticated_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        response = requests.post('http://127.0.0.1:5000/' + bank_unauthenticated_contract.contract_name + '/transfer', json={
            'inputs': ['{"name": "alice", "balance": 10}', '{"name": "bob", "balance": 10}'],
            'outputs': ['{"balance": 5, "name": "alice"}', '{"balance": 15, "name": "bob"}'],
            'parameters': ['5'],
            'reference_inputs': [],
            'returns': {},
        })
        response_json = response.json()
        self.assertTrue(response_json['success'])

        response = requests.post('http://127.0.0.1:5000/' + bank_unauthenticated_contract.contract_name + '/transfer', json={
            'inputs': ['{"balance": 10, "name": "alice"}', '{"balance": 10, "name": "bob"}'],
            'outputs': ['{"balance": 5, "name": "alice"}', '{"balance": 16, "name": "bob"}'],
            'parameters': ['5'],
            'reference_inputs': [],
            'returns': {},
        })
        response_json = response.json()
        self.assertFalse(response_json['success'])

        checker_service_process.terminate()
        checker_service_process.join()

    def test_increment_twice(self):
        checker_service_process = Process(target=increment_twice_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        response = requests.post('http://127.0.0.1:5000/' + increment_twice_contract.contract_name + '/increment', json={
            'inputs': ('0', ),
            'contractID': 0,
            'parameters': ['0'],
            'outputs': ('1', ),
            'returns': {},
            'dependencies': [{
                'inputs': ('0', ),
                'contractID': increment_contract.contract_name,
                'parameters': [],
                'outputs': ('1', ),
                'returns': {},
                'reference_inputs': ()
            }],
            'reference_inputs': ()
        })
        response_json = response.json()
        self.assertTrue(response_json['success'])

        response = requests.post('http://127.0.0.1:5000/' + increment_twice_contract.contract_name + '/increment', json={
            'inputs': ('0', ),
            'contractID': 0,
            'parameters': ['0'],
            'outputs': ('1', ),
            'returns': {},
            'dependencies': [{
                'inputs': ('0', ),
                'contractID': increment_contract.contract_name,
                'parameters': [],
                'outputs': ('0', ),
                'returns': {},
                'reference_inputs': ()
            }],
            'reference_inputs': ()
        })
        response_json = response.json()
        self.assertFalse(response_json['success'])

        checker_service_process.terminate()
        checker_service_process.join()

    def test_increment_thrice(self):
        checker_service_process = Process(target=increment_thrice_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        response = requests.post('http://127.0.0.1:5000/' + increment_thrice_contract.contract_name + '/increment', json={
            'inputs': ('0', ),
            'contractID': 0,
            'parameters': ['0', '0'],
            'outputs': ('1', ),
            'returns': {},
            'dependencies': [{
                'inputs': ('0', ),
                'contractID': increment_twice_contract.contract_name,
                'parameters': ['0'],
                'outputs': ('1', ),
                'returns': {},
                'reference_inputs': ()
            }],
            'reference_inputs': ()
        })
        response_json = response.json()
        self.assertTrue(response_json['success'])

        response = requests.post('http://127.0.0.1:5000/' + increment_thrice_contract.contract_name + '/increment', json={
            'inputs': ('0', ),
            'contractID': 0,
            'parameters': ['0', '0'],
            'outputs': ('1', ),
            'returns': {},
            'dependencies': [{
                'inputs': ('0', ),
                'contractID': increment_twice_contract.contract_name,
                'parameters': ['0'],
                'outputs': ('0', ),
                'returns': {},
                'reference_inputs': ()
            }],
            'reference_inputs': ()
        })
        response_json = response.json()
        self.assertFalse(response_json['success'])

        checker_service_process.terminate()
        checker_service_process.join()


if __name__ == '__main__':
    unittest.main()
