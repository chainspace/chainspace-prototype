package uk.ac.ucl.cs.sec.chainspace.bft;

import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TestMapClient {

    @Test
    public void how_it_maps_objects_to_shards() {


        int shardId = MapClient.objectToShardAlgorithm("4", 2);

        assertThat(shardId, is(1));
    }

}
