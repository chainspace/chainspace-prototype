import json
import sys

from matplotlib import pyplot

from chainspacemeasurements.results import parse_shard_results


def plot_shard_scaling(results1, results2, outfile):
    parsed_results1 = parse_shard_results(results1)
    if results2 != None: 
        parsed_results2 = parse_shard_results(results2)

    pyplot.xlabel('Number of shards')
    pyplot.ylabel('Average transactions / second')
    pyplot.grid(True)

    pyplot.errorbar(
        range(2, len(parsed_results1)+2),
        [i[0] for i in parsed_results1],
        [i[1] for i in parsed_results1],
        marker='o',
        #color='black',
    )
    if results2 != None: 
        pyplot.errorbar(
            range(2, len(parsed_results2)+2),
            [i[0] for i in parsed_results2],
            [i[1] for i in parsed_results2],
            marker='o',
            #color='black',
        )

    pyplot.savefig(outfile)
    pyplot.close()


def plot_input_scaling(results, outfile):
    parsed_results = parse_shard_results(results)
    pyplot.xlabel('Number of inputs per transaction')
    pyplot.ylabel('Average transactions / second')
    pyplot.grid(True)

    pyplot.errorbar(
        range(1, len(parsed_results)+1),
        [i[0] for i in parsed_results],
        [i[1] for i in parsed_results],
        marker='o',
    )

    pyplot.savefig(outfile)
    pyplot.close()


def plot_node_scaling(results, outfile, step):
    parsed_results = parse_shard_results(results)
    pyplot.xlabel('Number of replicas per shard')
    pyplot.ylabel('Average transactions / second')
    pyplot.grid(True)

    pyplot.errorbar(
        range(4, 4+len(parsed_results)*step, step),
        [i[0] for i in parsed_results],
        [i[1] for i in parsed_results],
        marker='o',
    )

    pyplot.savefig(outfile)
    pyplot.close()


if __name__ == '__main__':
    if sys.argv[1] == 'shardscaling':
        results = json.loads(open(sys.argv[2]).read())
        plot_shard_scaling(results, None, sys.argv[3])
    if sys.argv[1] == 'shardscaling2':
        results1 = json.loads(open(sys.argv[2]).read())
        results2 = json.loads(open(sys.argv[3]).read())
        plot_shard_scaling(results1, results2, sys.argv[4])
    elif sys.argv[1] == 'inputscaling':
        results = json.loads(open(sys.argv[2]).read())
        plot_input_scaling(results, sys.argv[3])
    elif sys.argv[1] == 'nodescaling':
        results = json.loads(open(sys.argv[2]).read())
        plot_node_scaling(results, sys.argv[3], int(sys.argv[4]))
