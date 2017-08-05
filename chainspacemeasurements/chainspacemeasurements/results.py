import numpy

def parse_shard_scaling(results):
    final_result = []
    for shard in results:
        sum_set = []
        for tps_set in shard:
            sum_set.append(sum(tps_set))

        mean = numpy.mean(sum_set)
        sd = numpy.sd(sum_set)

        final_result.append((mean, sd))

    return final_result
