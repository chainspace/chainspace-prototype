import numpy

def parse_shard_scaling(results):
    final_result = []
    for shard in results:
        sum_set = []
        for tps_set in shard:
            sum_set.append(sum(tps_set))

        mean = numpy.mean(sum_set)
        sd = numpy.std(sum_set)

        #trimmed_sum_set = []
        #for s in sum_set:
        #    if mean + sd*2 > s and mean - sd*2 < s:
        #        trimmed_sum_set.append(s)
        #mean = numpy.mean(trimmed_sum_set)

        final_result.append((mean, sd))

    return final_result


def parse_input_scaling_2(results):
    final_result = []
    for shard in results:
        sum_set = []
        for tps_set in shard:
            sum_set.append(sum(tps_set))

        mean = numpy.mean(sum_set)
        sd = numpy.std(sum_set)

        final_result.append((mean, sd))

    return final_result


def parse_node_scaling(results):
    final_result = []
    for shard in results:
        sum_set = []
        for tps_set in shard:
            sum_set.append(sum(tps_set))

        mean = numpy.mean(sum_set)
        sd = numpy.std(sum_set)

        final_result.append((mean, sd))

    return final_result
