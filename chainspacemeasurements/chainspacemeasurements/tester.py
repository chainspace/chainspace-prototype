import os
import time

from chainspacemeasurements import dumper


class Tester(object):
    def __init__(self, core_directory, network):
        self.core_directory = core_directory
        self.network = network

        network.ssh_connect()

    def start_client(self):
        command = ''
        command += 'cd {0};'.format(self.core_directory)
        command += 'screen -dmS clientservice ./runclientservice.sh;'
        os.system(command)

    def stop_client(self):
        os.system('killall java')

    def do_measurements(self, inputs_per_tx, min_shards, max_shards):
        tps_sets = []
        for num_shards in range(min_shards, max_shards+1):
            self.network.config_core(num_shards, 4)
            self.network.config_me(self.core_directory + '/ChainSpaceClientConfig')
            self.network.start_core()

            self.start_client()
            time.sleep(3)
            dumper.simulation_b2(500, inputs_per_tx)
            time.sleep(10)
            self.stop_client()

            tps_sets.append(self.network.get_tps_set())

            self.network.stop_core()
            self.network.clean_core()

        return tps_sets


if __name__ == '__main__':
    from chainspacemeasurements.instances import ChainspaceNetwork
    n = ChainspaceNetwork(0)
    t = Tester('/home/admin/chainspace/chainspacecore', n)

    t.do_measurements(1, 2, 3)
