"""tcpdump parser"""


def parse_tcpdump(filename):
    lines = open(filename).readlines()

    txes = {}

    for i, line in enumerate(lines):
        if 'Flags' in line:
            time_line = line
        elif 'accepted_t_commit;' in line:
            hash_index = line.index('accepted_t_commit;')+len('accepted_t_commit;')
            first_input = line[hash_index:hash_index+64]
            timestamp = float(time_line[:17])
            timestamp = timestamp*1000
            txes[first_input] = timestamp

    return txes
