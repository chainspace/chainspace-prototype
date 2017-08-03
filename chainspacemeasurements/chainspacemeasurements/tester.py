import os
import time

from chainspacemeasurements import dumper


class Tester(object):
    def __init__(self, core_directory, network):
        self.core_directory = core_directory
        self.network = network
        self.min_shards = min_shards
        self.max_shards = max_shards

        network.ssh_connect()

    def start_client(self):
        command = ''
        command += 'cd {0};'.format(core_directory)
        command += 'screen -dmS clientservice ./runclientservice.sh;'

    def stop_client(self):
        os.system('killall java')

    def do_measurements(self, inputs_per_tx, min_shards, max_shards):
        tps_sets = []
        for num_shards in range(min_shards, max_shards):
            network.config_core(num_shards, 4)
            network.config_me(core_directory + '/ChainSpaceClientConfig')
            network.start_core()

            self.start_client()
            time.sleep(3)
            dumper.simulation_b2(500, inputs_per_tx)
            self.stop_client()

            tps_sets.append(network.get_tps_set())

            network.stop_core()
            network.clean_core()

        return tps_sets
