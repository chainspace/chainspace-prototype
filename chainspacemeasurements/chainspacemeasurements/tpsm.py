"""Script to calculate transactions per second from an SQLite database."""


def tps(filename):
    lines = open(filename).readlines()
    lines = lines[2:]
    lines = [line.strip() for line in lines]

    start_time = int(lines[0].split()[0])
    end_time = int(lines[-1].split()[0])
    s = (end_time - start_time) / float(1000)

    tx_num = 0
    for line in lines:
        record = line.split()
        num_shards = int(record[2])
        tx_num += 1/float(num_shards)

    tps = tx_num / float(s)

    return tps


if __name__ == '__main__':
    try:
        print tps('simplelog')
    except Exception:
        print '0'
