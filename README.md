# Chainspace

## Developer Installation

The bulk of the code is to be found in `chainspacecore`. To run a network of chainspace nodes, you need to first compile and package this module.

```
cd chainspacecore
mvn -Dversion=1.0-SNAPSHOT package assembly:single
```

This should produce an "uber jar" in the folder `chainspacecore/target`

This will show you all running chainspace processes:

ps aux | grep -v grep | grep chainspace | awk '{print $2}'

If you need to kill everything:


ps aux | grep -v grep | grep chainspace | awk '{print $2}' | xargs kill
