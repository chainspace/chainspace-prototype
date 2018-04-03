# System Tests

These python files perform a series of whole leve system tests.

They assume (for now) that the client api at least is running locally on 127.0.0.1:5000

They also assume you have the contracts installed in your chainspace node:

```
petition_encrypted.py
```

To execute, first make sure you setup the virtualenve for python then
```
python test_petition_encrypted.py
```

Should run through a bunch of transactions and tell you at the end wether they worked or not.

## Setup environmnet

So how