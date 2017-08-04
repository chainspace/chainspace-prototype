""" test authenticated bank transfer """

####################################################################
# imports
###################################################################
# general
from multiprocessing import Process
from json            import dumps, loads
import time
import unittest
import requests
# chainspace
from chainspacecontract import transaction_to_solution
from chainspacecontract.examples.sensor import contract as sensor_contract
from chainspacecontract.examples import sensor


####################################################################
# authenticated bank transfer
####################################################################
class TestSensor(unittest.TestCase):
    # --------------------------------------------------------------
    # test init
    # --------------------------------------------------------------
    def test_init(self):
        ##
        ## run service
        ##
        checker_service_process = Process(target=sensor_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        transaction_dict = sensor.init()

        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + sensor_contract.contract_name + '/init', json=transaction_to_solution(transaction_dict)
        )
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        checker_service_process.terminate()
        checker_service_process.join()


    # --------------------------------------------------------------
    # test create sensor
    # --------------------------------------------------------------
    def test_create_sensor(self):
        ##
        ## run service
        ##
        checker_service_process = Process(target=sensor_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##

        # init
        init_transaction = sensor.init()['transaction']
        token = init_transaction['outputs'][0]

        # create bank account
        transaction_dict = sensor.create_sensor(
            (token,),
            None,
            None,
        )

        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + sensor_contract.contract_name + '/create_sensor', json=transaction_to_solution(transaction_dict)
        )
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        checker_service_process.terminate()
        checker_service_process.join()



    # --------------------------------------------------------------
    # test add sensor data
    # --------------------------------------------------------------
    def test_add_data(self):
        ##
        ## run service
        ##
        checker_service_process = Process(target=sensor_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        # init
        init_transaction = sensor_contract.init()['transaction']
        token = init_transaction['outputs'][0]

        # create alice's account
        create_sensor_transaction = sensor_contract.create_sensor(
            (token,),
            None,
            None,
        )
        sensor = create_sensor_transaction['transaction']['outputs'][1]

        # pack transaction
        transaction_dict = sensor_contract.add_data(
            (sensor,),
            None,
            [dumps([1, 2, 3])],
        )

        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + sensor_contract.contract_name + '/add_data', json=transaction_to_solution(transaction_dict)
        )
        self.assertTrue(response.json()['success'])


        ##
        ## stop service
        ##
        checker_service_process.terminate()
        checker_service_process.join()


    # --------------------------------------------------------------
    # test read account
    # --------------------------------------------------------------
    def test_read_data(self):
        ##
        ## run service
        ##
        checker_service_process = Process(target=sensor_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        # init
        init_transaction = sensor_contract.init()['transaction']
        token = init_transaction['outputs'][0]

        # create sensor
        create_sensor_transaction = sensor_contract.create_sensor(
            (token,),
            None,
            None,
        )
        sensor = create_sensor_transaction['transaction']['outputs'][1]

        # pack transaction
        add_data_transaction = sensor_contract.add_data(
            (sensor,),
            None,
            [dumps([1, 2, 3])],
        )

        # read data 
        new_sensor = add_data_transaction['transaction']['outputs'][0]
        read_transaction = sensor_contract.read(
            None,
            (new_sensor,),
            None
        )
        print read_transaction['transaction']['returns']


        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + sensor_contract.contract_name + '/read', json=transaction_to_solution(read_transaction)
        )
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        checker_service_process.terminate()
        checker_service_process.join()


####################################################################
# main
###################################################################
if __name__ == '__main__':
    unittest.main()
