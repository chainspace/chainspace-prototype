""" test authenticated bank transfer """

####################################################################
# imports
###################################################################
# general
from multiprocessing import Process
# chainspace
from chainspacecontract.examples.bank_authenticated import contract as bank_authenticated_contract


####################################################################
# run checker
###################################################################
def run_checker():
        ##
        ## run service
        ##

        file = open("/Users/alberto/Desktop/test.txt","w")
        file.write("Hello")
        file.close()

        checker_service_process = Process(target=bank_authenticated_contract.run_checker_service)
        checker_service_process.start()
        

        ##
        ## stop service
        ##
        """
        checker_service_process.terminate()
        checker_service_process.join()
        """

####################################################################
# main
###################################################################
if __name__ == '__main__':
    run_checker()
