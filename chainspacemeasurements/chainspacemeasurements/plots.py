import json
import sys

from matplotlib import pyplot

from chainspacemeasurements.results import parse_shard_scaling


def plot_shard_scaling(results, outfile):
    parsed_results = parse_shard_scaling(results)
    pyplot.xlabel('Number of shards')
    pyplot.ylabel('Transactions / sec')
    pyplot.grid(True)

    pyplot.plot(
        range(2, len(parsed_results)+2),
        [i[0] for i in parsed_results]
    )

    pyplot.savefig(outfile)
    pyplot.close()


if __name__ == '__main__':
    if sys.argv[1] == 'shardscaling':
        results = json.loads(open(sys.argv[2]).read())
        plot_shard_scaling(results, sys.argv[3])
