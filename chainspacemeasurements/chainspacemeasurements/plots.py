from matplotlib import pyplot


pyplot.xlabel('Number of shards')
pyplot.ylabel('Transactions / sec')
pyplot.grid(True)
pyplot.savefig('test.pdf')
pyplot.close()
