"""EC2 instance management."""
import boto3
import paramiko

ec2 = boto3.resource('ec2', region_name='us-east-2')


class ChainspaceSystem(object):
    def __init__(self, realm):
        self.realm = str(realm)

        self.ssh_connections = {}

    def _get_instances(self):
        return ec2.instances.filter(Filters=[
            {'Name': 'tag:realm', 'Values': [self.realm]},
            {'Name': 'instance-state-name', 'Values': ['running']}
        ])

    def bootstrap(self, shards, nodes_per_shard, key_name):
        for i in range(shards):
            for j in range(nodes_per_shard):
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
                                {'Key': 'shard', 'Value': str(i)},
                                {'Key': 'node', 'Value': str(j)},
                            ]
                        }
                    ]
                )

    def get_ips(self):
        return [instance.public_ip_address for instance in self._get_instances()]

    def terminate(self):
        self._get_instances().terminate()

    def ssh_connect(self):
        for instance in self._get_instances():
            client = paramiko.SSHClient()
            client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
            client.connect(hostname=instance.public_ip_address, username='admin')
            self.ssh_connections[instance] = client

    def ssh_exec_command(self, command):
        for instance, client in self.ssh_connections.items():
            stdin, stdout, stderr = client.exec_command(command)

            instance_tags = dict(map(lambda x: (x['Key'], x['Value']), instance.tags or []))
            print '[shard ' + instance_tags['shard'] + ' node ' + instance_tags['node'] + '] ' + stdout.read() + stderr.read(),

    def ssh_close(self):
        for instance, client in self.ssh_connections.items():
            client.close()
