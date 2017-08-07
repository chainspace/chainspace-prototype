import os
import operator
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
        network.stop_core()
        time.sleep(2)
        network.clean_state_core()

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
                                self.network.ssh_close()
                                self.network.ssh_connect()
                                self.network.stop_core()
                                time.sleep(2)
                                self.network.clean_state_core()
                                break
                            except:
                                time.sleep(5)

            tps_sets_sets.append(tps_sets)

        self.outfh.write(json.dumps(tps_sets_sets))
        return tps_sets_sets

    def measure_input_scaling(self, num_shards, min_inputs, max_inputs, runs):
        tps_set_set = []
        for num_inputs in range(min_inputs, max_inputs+1):
            tps_set = []

            for i in range(runs):
                try:
                    print "Running measurements for {2} inputs across {0} shards (run {1}).".format(num_shards, i, num_inputs)
                    self.network.config_core(num_shards, 4)
                    self.network.config_me(self.core_directory + '/ChainSpaceClientConfig')
                    self.network.start_core()

                    batch_size = 100*num_shards
                    num_transactions = 300*num_shards

                    time.sleep(10)
                    self.start_client()
                    time.sleep(10)
                    dumper.simulation_batched(num_transactions, num_inputs, batch_size=batch_size, batch_sleep=1)
                    time.sleep(20)
                    self.stop_client()

                    txes = {}
                    logs = self.network.get_r0_logs()
                    for log in logs:
                        for line in log.splitlines():
                            line = line.strip()
                            if line == '':
                                continue
                            records = line.split()
                            if records[1] not in txes:
                                txes[records[1]] = int(records[0])

                    sorted_txes = sorted(txes.items(), key=operator.itemgetter(1))
                    sorted_txes = sorted_txes[(len(sorted_txes)/10):]
                    tps = ((len(sorted_txes)-2)*1000) / (int(sorted_txes[-1][1]) - int(sorted_txes[2][1]))
                    tps_set.append(tps)
                    print "Result: {0}".format(tps)
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
                                self.network.ssh_close()
                                self.network.ssh_connect()
                                self.network.stop_core()
                                time.sleep(2)
                                self.network.clean_state_core()
                                break
                            except:
                                time.sleep(5)

            tps_set_set.append(tps_set)

        self.outfh.write(json.dumps(tps_set_set))
        return tps_set_set

    def measure_input_scaling_2(self, num_shards, min_inputs, max_inputs, runs):
        tps_sets_sets = []
        for num_inputs in range(min_inputs, max_inputs+1):
            tps_sets = []

            for i in range(runs):
                try:
                    print "Running measurements for {2} inputs across {0} shards (run {1}).".format(num_shards, i, num_inputs)
                    self.network.config_core(num_shards, 4)
                    self.network.config_me(self.core_directory + '/ChainSpaceClientConfig')
                    self.network.start_core()

                    batch_size = 100*num_shards
                    num_transactions = 300*num_shards

                    time.sleep(10)
                    self.start_client()
                    time.sleep(10)
                    dumper.simulation_batched(num_transactions, num_inputs, batch_size=batch_size, batch_sleep=1)
                    time.sleep(20)
                    self.stop_client()

                    txes = {}
                    logs = self.network.get_r0_logs()
                    for log in logs:
                        for line in log.splitlines():
                            line = line.strip()
                            if line == '':
                                continue
                            records = line.split()
                            if records[1] not in txes:
                                txes[records[1]] = int(records[0])

                    tps_set = self.network.get_tpsm_set()
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
                                self.network.ssh_close()
                                self.network.ssh_connect()
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

    elif sys.argv[1] == 'inputscaling':
        num_shards = int(sys.argv[2])
        min_inputs = int(sys.argv[3])
        max_inputs = int(sys.argv[4])
        runs = int(sys.argv[5])
        outfile = sys.argv[6]

        n = ChainspaceNetwork(0)
        t = Tester(n, outfile=outfile)

        print t.measure_input_scaling_2(num_shards, min_inputs, max_inputs, runs)
    elif sys.argv[1] == 'inputscaling2':
        num_shards = int(sys.argv[2])
        min_inputs = int(sys.argv[3])
        max_inputs = int(sys.argv[4])
        runs = int(sys.argv[5])
        outfile = sys.argv[6]

        n = ChainspaceNetwork(0)
        t = Tester(n, outfile=outfile)

        print t.measure_input_scaling_2(num_shards, min_inputs, max_inputs, runs)
