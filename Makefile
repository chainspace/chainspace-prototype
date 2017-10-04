
ps:
	ps aux | grep -v grep | grep chainspace

list-nodes:
	screen -list

kill-all:
	ps aux | grep -v grep | grep chainspace | awk '{print $2}' | xargs kill
