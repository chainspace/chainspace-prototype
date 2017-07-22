"""EC2 instance management."""
import sys
from multiprocessing.dummy import Pool

import boto3
import paramiko

ec2 = boto3.resource('ec2', region_name='us-east-2')


class ChainspaceNetwork(object):
    threads = 20

    def __init__(self, realm):
        self.realm = str(realm)

        self.ssh_connections = {}

    def _get_instances(self):
        return ec2.instances.filter(Filters=[
            {'Name': 'tag:type', 'Values': ['chainspace']},
            {'Name': 'tag:realm', 'Values': [self.realm]},
            {'Name': 'instance-state-name', 'Values': ['running']}
        ])

    def _log(self, message):
        _safe_print(message)

    def _log_instance(self, instance, message):
        instance_tags = _tags_from_instance(instance)
        message = '[shard {} node {}] {}'.format(instance_tags['shard'], instance_tags['node'], message)
        self._log(message)

    def _get_bootstrap_commands(self, instance):
        commands = (
            'sudo apt update',
            'sudo apt install git python-pip -y',
            'git clone https://github.com/musalbas/chainspace',
            'sudo pip install chainspace/chainspacecontract'
        )
        return commands

    def _single_bootstrap(self, instance):
        for command in self._get_bootstrap_commands(instance):
            self._single_ssh_exec(instance, command)

    def _single_start(self, shard, node, key_name):
        self._log("Starting node {} in shard {}...".format(node, shard))
        shard = str(shard)
        node = str(node)
        ec2.create_instances(
            ImageId='ami-b2795cd7', # Debian 8.7
            InstanceType='t2.micro',
            MinCount=1,
            MaxCount=1,
            KeyName=key_name,
            SecurityGroups=['ssh-all-in'],
            TagSpecifications=[
                {
                    'ResourceType': 'instance',
                    'Tags': [
                        {'Key': 'type', 'Value': 'chainspace'},
                        {'Key': 'realm', 'Value': self.realm},
                        {'Key': 'shard', 'Value': shard},
                        {'Key': 'node', 'Value': node},
                        {'Key': 'Name', 'Value': 'Chainspace - network: {} shard: {} node: {}'.format(self.realm, shard, node)},
                    ]
                }
            ]
        )

        self._log("Started node {} in shard {}.".format(node, shard))

    def _single_ssh_connect(self, instance):
        self._log_instance(instance, "Initiating SSH connection...")
        client = paramiko.SSHClient()
        client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        client.connect(hostname=instance.public_ip_address, username='admin')
        self.ssh_connections[instance] = client
        self._log_instance(instance, "Initiated SSH connection.")

    def _single_ssh_exec(self, instance, command):
        self._log_instance(instance, "Command: {}".format(command))
        client = self.ssh_connections[instance]
        stdin, stdout, stderr = client.exec_command(command)
        for message in stdout.readlines():
            self._log_instance(instance, message.rstrip())
        for message in stderr.readlines():
            self._log_instance(instance, message.rstrip())

    def _single_ssh_close(self, instance):
        self._log_instance(instance, "Closing SSH connection...")
        client = self.ssh_connections[instance]
        client.close()
        self._log_instance(instance, "Closed SSH connection.")

    def start(self, shards, nodes_per_shard, key_name):
        args = []
        for i in range(shards):
            for j in range(nodes_per_shard):
                args.append((self._single_start, i, j, key_name))

        pool = Pool(ChainspaceNetwork.threads)
        pool.map(_multi_args_wrapper, args)
        pool.close()
        pool.join()

    def bootstrap(self):
        args = [(self._single_bootstrap, instance) for instance in self._get_instances()]
        pool = Pool(ChainspaceNetwork.threads)
        pool.map(_multi_args_wrapper, args)
        pool.close()
        pool.join()

    def ssh_connect(self):
        args = [(self._single_ssh_connect, instance) for instance in self._get_instances()]
        pool = Pool(ChainspaceNetwork.threads)
        pool.map(_multi_args_wrapper, args)
        pool.close()
        pool.join()

    def ssh_exec(self, command):
        args = [(self._single_ssh_exec, instance, command) for instance in self._get_instances()]
        pool = Pool(ChainspaceNetwork.threads)
        pool.map(_multi_args_wrapper, args)
        pool.close()
        pool.join()

    def ssh_close(self):
        args = [(self._single_ssh_close, instance) for instance in self._get_instances()]
        pool = Pool(ChainspaceNetwork.threads)
        pool.map(_multi_args_wrapper, args)
        pool.close()
        pool.join()

    def get_ips(self):
        return [instance.public_ip_address for instance in self._get_instances()]

    def terminate(self):
        self._log("Terminating all nodes...")
        self._get_instances().terminate()
        self._log("All nodes terminated.")


def _tags_from_instance(instance):
    return dict(map(lambda x: (x['Key'], x['Value']), instance.tags or []))


def _multi_args_wrapper(args):
    return args[0](*args[1:])


def _safe_print(message):
    print "{0}\n".format(message),
    sys.stdout.flush()
