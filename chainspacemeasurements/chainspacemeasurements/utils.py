from hashlib import sha256


def map_object_to_shard(num_shards, value):
    h = sha256(value).hexdigest()
    return (h % num_shards)
