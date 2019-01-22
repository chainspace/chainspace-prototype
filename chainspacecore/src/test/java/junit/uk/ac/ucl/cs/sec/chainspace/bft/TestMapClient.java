package uk.ac.ucl.cs.sec.chainspace.bft;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TestMapClient {

    @Test
    public void how_it_maps_objects_to_shards() {

        test_shard_id("cfe08a142af2899b9177adeb85e30b613cf0d36bf7e492a4146fa1faf71330bc", 0, 2);
        test_shard_id("404d5c43cf0f34857b65405cb2cdcf015cbe792ee0327157f1cd66ea8b340411", 1, 2);

        test_shard_id("16b5467c7759e8534f9663305eef8dc82818bd2027232bdfb8786bf8e82d7bf6", 0, 2);
    }

    private void test_shard_id(String object_id, int expectedShard, int numShards) {
        int shardId = MapClient.objectToShardAlgorithm(object_id, numShards);

        assertThat(object_id, shardId, is(expectedShard));
    }

}
