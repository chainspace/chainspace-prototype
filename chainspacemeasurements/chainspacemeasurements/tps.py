"""Script to calculate transactions per second from an SQLite database."""
import sqlite3


def tps(con):
    cur = con.cursor()
    cur.execute('SELECT strftime("%s", time_stamp) from logs')

    tx_count = 0
    first_tx = 0
    last_tx = 0
    for log in cur.fetchall():
        tx_count += 1
        timestamp = int(log[0])
        if first_tx > timestamp or first_tx == 0:
            first_tx = timestamp
        if last_tx < timestamp:
            last_tx = timestamp

    tps = float(tx_count) / (last_tx - first_tx)
    return tps


if __name__ == '__main__':
    con = sqlite3.connect('database.sqlite')
    print tps(con)
