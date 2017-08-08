from hashlib import sha256


def map_object_to_shard(num_shards, value):
    h = sha256(value).hexdigest()
    h = int(h, 16)
    return (h % num_shards)
