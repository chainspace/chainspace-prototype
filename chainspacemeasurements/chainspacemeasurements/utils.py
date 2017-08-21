import datetime
from hashlib import sha256


def map_object_id_to_shard(num_shards, object_id):
    i = int(object_id, 16)
    return (i % num_shards)
