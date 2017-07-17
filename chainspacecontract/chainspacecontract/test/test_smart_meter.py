""" test authenticated bank transfer """

####################################################################
# imports
###################################################################
# general
from multiprocessing import Process
import time
import unittest
import requests
# chainsapce
from chainspacecontract.examples.smart_meter import contract as smart_meter_contract
from chainspacecontract.examples import smart_meter
# crypto
from chainspacecontract.examples.utils import setup, key_gen, pack


####################################################################
# authenticated bank transfer
####################################################################
class TestBankAuthenticated(unittest.TestCase):
    # --------------------------------------------------------------
    # test init
    # --------------------------------------------------------------
    def test_init(self):
        ##
        ## run service
        ##
        checker_service_process = Process(target=smart_meter_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        transaction = smart_meter.init()

        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + smart_meter_contract.contract_name + '/init', json=transaction
        )
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        checker_service_process.terminate()
        checker_service_process.join()

    # --------------------------------------------------------------
    # test create meter
    # --------------------------------------------------------------
    def test_create_meter(self):
        ##
        ## run service
        ##
        checker_service_process = Process(target=smart_meter_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        # create provider's public key
        (_, provider_pub) = key_gen(setup())

        # init
        init_transaction = smart_meter.init()
        token = init_transaction['outputs'][0]

        # create meter
        transaction = smart_meter.create_meter(
            (token,),
            None,
            None,
            pack(provider_pub),
            'Some info about the meter.',   # some info about the meter
            [5, 3, 5, 3, 5],                # the tariffs 
            764                             # the billing period
        )

        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + smart_meter_contract.contract_name + '/create_meter', json=transaction
        )
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        checker_service_process.terminate()
        checker_service_process.join()

    # --------------------------------------------------------------
    # test add reading
    # --------------------------------------------------------------
    def test_add_reading(self):
        ##
        ## run service
        ##
        checker_service_process = Process(target=smart_meter_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        # generate crypto params
        G = setup()[0]
        (provider_priv, provider_pub) = key_gen(setup())

        # init
        init_transaction = smart_meter.init()
        token = init_transaction['outputs'][0]

        # create meter
        create_meter_transaction = smart_meter.create_meter(
            (token,),
            None,
            None,
            pack(provider_pub),
            'Some info about the meter.',
            [5, 3, 5, 3, 5],  # the tariffs
            764               # billing period         
        )
        meter = create_meter_transaction['outputs'][1]

        # add a reading
        transaction = smart_meter.add_reading(
            (meter,),
            None,
            None,
            pack(provider_priv),
            10,                  # the new reading 
            G.order().random()   # the opening value
        )


        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + smart_meter_contract.contract_name + '/add_reading', json=transaction
        )
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        checker_service_process.terminate()
        checker_service_process.join()

    # --------------------------------------------------------------
    # test add many reading
    # --------------------------------------------------------------
    def test_add_many_reading(self):
        ##
        ## run service
        ##
        checker_service_process = Process(target=smart_meter_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        # generate crypto params
        G = setup()[0]
        (provider_priv, provider_pub) = key_gen(setup())

        # set init values
        tariffs  = [5, 3, 5, 3, 5]
        readings = [10, 20, 30, 10, 50]
        openings = [G.order().random() for _ in tariffs]
        billing_period = 764

        # init
        init_transaction = smart_meter.init()
        token = init_transaction['outputs'][0]

        # create meter
        create_meter_transaction = smart_meter.create_meter(
            (token,),
            None,
            None,
            pack(provider_pub),
            'Some info about the meter.',
            tariffs,
            billing_period        
        )
        meter = create_meter_transaction['outputs'][1]

        # add a readings
        transaction = {}
        input_obj = meter
        for i in range(0, len(tariffs)):
            transaction = smart_meter.add_reading(
                (input_obj,),
                None,
                None,
                pack(provider_priv),
                readings[i],
                openings[i]
            )
            input_obj = transaction['outputs'][0]


        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + smart_meter_contract.contract_name + '/add_reading', json=transaction
        )
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        checker_service_process.terminate()
        checker_service_process.join()

    # --------------------------------------------------------------
    # test compute bill
    # --------------------------------------------------------------
    def test_compute_bill(self):
        ##
        ## run service
        ##
        checker_service_process = Process(target=smart_meter_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        # generate crypto params
        G = setup()[0]
        (provider_priv, provider_pub) = key_gen(setup())

        # set init values
        tariffs  = [5, 3, 5, 3, 5]
        readings = [10, 20, 30, 10, 50]
        openings = [G.order().random() for _ in tariffs]
        billing_period = 764

        # create provider's public key
        (provider_priv, provider_pub) = key_gen(setup())

        # init
        init_transaction = smart_meter.init()
        token = init_transaction['outputs'][0]

        # create meter
        create_meter_transaction = smart_meter.create_meter(
            (token,),
            None,
            None,
            pack(provider_pub),
            'Some info about the meter.',
            tariffs,
            billing_period                
        )
        meter = create_meter_transaction['outputs'][1]

        # add a readings
        transaction = {}
        input_obj = meter
        for i in range(0, len(tariffs)):
            transaction = smart_meter.add_reading(
                (input_obj,),
                None,
                None,
                pack(provider_priv),
                readings[i],
                openings[i]
            )
            input_obj = transaction['outputs'][0]

        # compute bill
        transaction = smart_meter.compute_bill(
            (input_obj,),
            None,
            None,
            readings, 
            openings, 
            tariffs              
        )


        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + smart_meter_contract.contract_name + '/compute_bill', json=transaction
        )
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        checker_service_process.terminate()
        checker_service_process.join()

    # --------------------------------------------------------------
    # test read bill
    # --------------------------------------------------------------
    def test_read_bill(self):
        ##
        ## run service
        ##
        checker_service_process = Process(target=smart_meter_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        # generate crypto params
        G = setup()[0]
        (provider_priv, provider_pub) = key_gen(setup())

        # set init values
        tariffs  = [5, 3, 5, 3, 5]
        readings = [10, 20, 30, 10, 50]
        openings = [G.order().random() for _ in tariffs]
        billing_period = 764

        # create provider's public key
        (provider_priv, provider_pub) = key_gen(setup())

        # init
        init_transaction = smart_meter.init()
        token = init_transaction['outputs'][0]

        # create meter
        create_meter_transaction = smart_meter.create_meter(
            (token,),
            None,
            None,
            pack(provider_pub),
            'Some info about the meter.',
            tariffs,
            billing_period                
        )
        meter = create_meter_transaction['outputs'][1]

        # add a readings
        transaction = {}
        input_obj = meter
        for i in range(0, len(tariffs)):
            transaction = smart_meter.add_reading(
                (input_obj,),
                None,
                None,
                pack(provider_priv),
                readings[i],
                openings[i]
            )
            input_obj = transaction['outputs'][0]

        # compute bill
        transaction = smart_meter.compute_bill(
            (input_obj,),
            None,
            None,
            readings, 
            openings, 
            tariffs              
        )
        bill = transaction['outputs'][0]

        # read bill
        transaction = smart_meter.read(
            None,
            (bill,),
            None
        )

        # print bill
        print transaction['returns']


        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + smart_meter_contract.contract_name + '/read', json=transaction
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