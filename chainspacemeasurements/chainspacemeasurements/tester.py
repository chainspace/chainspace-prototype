import os
import time
import sys
import traceback
import json

from chainspacemeasurements import dumper
from chainspacemeasurements.instances import ChainspaceNetwork


class Tester(object):
    def __init__(self, network, core_directory='/home/admin/chainspace/chainspacecore', outfile='out'):
        self.outfh = open(outfile, 'a')
        self.core_directory = core_directory
        self.network = network

        network.logging = False

        network.ssh_connect()

        # freshen state
        self.stop_client()
        n.stop_core()
        time.sleep(2)
        n.clean_state_core()

    def start_client(self):
        command = ''
        command += 'cd {0};'.format(self.core_directory)
        command += 'screen -dmS clientservice ./runclientservice.sh;'
        os.system(command)

    def stop_client(self):
        os.system('killall java')

    def measure_shard_scaling(self, min_shards, max_shards, runs):
        tps_sets_sets = []
        for num_shards in range(min_shards, max_shards+1):
            tps_sets = []

            for i in range(runs):
                try:
                    print "Running measurements for {0} shards (run {1}).".format(num_shards, i)
                    self.network.config_core(num_shards, 4)
                    self.network.config_me(self.core_directory + '/ChainSpaceClientConfig')
                    self.network.start_core()

                    batch_size = 100*num_shards
                    num_transactions = 300*num_shards

                    time.sleep(10)
                    self.start_client()
                    time.sleep(10)
                    dumper.simulation_batched(num_transactions, 1, batch_size=batch_size, batch_sleep=1)
                    time.sleep(20)
                    self.stop_client()

                    tps_set = self.network.get_tps_set()
                    tps_sets.append(tps_set)
                    print "Result for {0} shards (run {1}): {2}".format(num_shards, i, tps_set)
                except Exception:
                    traceback.print_exc()
                finally:
                    try:
                        self.network.stop_core()
                        time.sleep(2)
                        self.network.clean_state_core()
                    except:
                        # reset connection
                        for i in range(5):
                            try:
                                self.n.ssh_close()
                                self.n.ssh_connect()
                                self.network.stop_core()
                                time.sleep(2)
                                self.network.clean_state_core()
                                break
                            except:
                                time.sleep(5)

            tps_sets_sets.append(tps_sets)

        self.outfh.write(json.dumps(tps_sets_sets))
        return tps_sets_sets


if __name__ == '__main__':
    if sys.argv[1] == 'shardscaling':
        min_shards = int(sys.argv[2])
        max_shards = int(sys.argv[3])
        runs = int(sys.argv[4])
        outfile = sys.argv[5]

        n = ChainspaceNetwork(0)
        t = Tester(n, outfile=outfile)

        print t.measure_shard_scaling(min_shards, max_shards, runs)
