from setuptools import setup


setup(
    name = 'chainspacemeasurements',
    version = '0.1',
    packages = ['chainspacemeasurements'],
    install_requires = [
        'paramiko',
        'boto3',
    ],
)
