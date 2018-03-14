# Chainspace Persistence

Each chainspace node stores a copy of the ledger in a local database. The current implementation is using sqlite.

In the directory where the node is running, you should find a file called `database.sqlite`

You can interact with this database on the command line, using the program `sqlite3`. (`brew install sqlite`).

Open one of the db files using `open <filename>`, e.g. `.open chainspacecore-1-0/database.sqlite`

Quit the session by `.exit`.

```
$ sqlite3
SQLite version 3.8.10.2 2015-05-20 18:17:19
Enter ".help" for usage hints.
Connected to a transient in-memory database.
Use ".open FILENAME" to reopen on a persistent database.
sqlite> .open chainspacecore-1-0/database.sqlite
sqlite> .tables
data  head  logs
```

You can see the schema of the whole db or a single table like this:

```
sqlite> .schema
CREATE TABLE data (object_id CHAR(32) NOT NULL,object TEXT NOT NULL,status INTEGER NOT NULL);
CREATE TABLE logs (time_stamp TIMESTAMP DEFAULT (STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW')),transaction_id CHAR(32) NOT NULL,transaction_json TEXT NOT NULL);
CREATE TABLE head (ID INTEGER PRIMARY KEY,digest CHAR(32) NOT NULL);
```

```
sqlite> .schema data
CREATE TABLE data (object_id CHAR(32) NOT NULL,object TEXT NOT NULL,status INTEGER NOT NULL);
```




