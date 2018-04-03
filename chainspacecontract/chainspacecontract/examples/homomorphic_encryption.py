# -*- coding: utf-8 -*-
"""

# Python privacy aware smart contracts with petlib


All of the code here comes from the [DECODE](https://decodeproject.eu/) project and in particular from [Chainspace](http://chainspace.io/) contributed by UCL from the team led by [George Danezis](http://www0.cs.ucl.ac.uk/staff/G.Danezis/) you can see the the code in action in the example smart contracts, [utils.py](https://github.com/chainspace/chainspace/blob/master/chainspacecontract/chainspacecontract/examples/utils.py) and [vote.py](https://github.com/chainspace/chainspace/blob/master/chainspacecontract/chainspacecontract/examples/vote.py)

It is worth reading the [Chainspace whitepaper](https://arxiv.org/abs/1708.03778) for more information.

These examples use the underlying library [petlib](http://petlib.readthedocs.io/en/latest/) contributed by George and his team.

Some background reading relevant to blockchains can be found in the way bitcoin utilises [Elliptic Curve Cryptography (ECC)](https://en.bitcoin.it/wiki/How_bitcoin_works#Cryptography)

You will need the following pre-requisits:

```
apt-get -qq install -y libssl-dev libffi-dev
```

```
pip install petlib
```
"""

from hashlib 		  import sha256, sha1
from binascii 		import hexlify, unhexlify

from petlib.bn    import Bn
from petlib.ec    import EcGroup
from petlib.ecdsa import do_ecdsa_setup, do_ecdsa_sign, do_ecdsa_verify
from petlib.pack  import encode, decode

"""Setup some useful utilities for dealing with keys and hashes and turning them to and from strings

```
H(x) #sha256 hash
pack(x) #encode and turn into hex x
unpack(x) #unhex and decode x
```
"""


def H(x):
    return hexlify(sha256(x).digest())


def pack(x):
    return hexlify(encode(x))


def unpack(x):
    return decode(unhexlify(x))

"""Some primary utils for setting up our cryptosystem

```
setup() #initialise the crypto coordinafes for th elliptic curves
key_gen() #generate a keypair
```
"""


def setup(nid=713):
    """ generates cryptosystem parameters """
    G = EcGroup(nid)
    g = G.generator()
    hs = [G.hash_to_point(("h%s" % i).encode("utf8")) for i in range(4)]
    o = G.order()
    return (G, g, hs, o)


def key_gen(params):
    """ generate a private / public key pair """
    (G, g, hs, o) = params
    priv = o.random()
    pub = priv * g
    return (priv, pub)

"""An important part of the setup is selecting the rigth elliptic curve to use - so as to match with other systems that want to verify your signature.

You can see what curves petlib supports like this:
"""

EcGroup.list_curves()

"""The default in petlib http://petlib.readthedocs.io/en/latest/#module-petlib-ec  is 713, which is 'NIST/SECG curve over a 224 bit prime field'

If we want interaction with milagro we will need to usea curve that it supports, like from https://github.com/milagro-crypto/milagro-crypto-js

One that it supports is NIST256 which in openssl (what petlib uses underneath) is listed as "prime256v1: X9.62/SECG curve over a 256 bit prime field", from https://security.stackexchange.com/questions/78621/which-elliptic-curve-should-i-use

Which would be item 415 in petlib

Ok, lets do some crypto!!
"""

params = setup()

(private_key, public_key) = key_gen(params)

print private_key
print public_key
print pack(public_key)
print pack(private_key)

"""What is happening here is that the `key_gen` function is generating an [ECDSA](https://en.wikipedia.org/wiki/Elliptic_Curve_Digital_Signature_Algorithm) (Elliptic Curve Digital Signature Algorithm) public and private key pair. You can see some more details in the documentation of petlib [here](http://petlib.readthedocs.io/en/latest/#module-petlib.ecdsa) which is what the `key_gen` function is doing.

See also http://andrea.corbellini.name/2015/05/30/elliptic-curve-cryptography-ecdh-and-ecdsa/

The first thing we are going to do is sign a peice of text. Signing is an important foundational part of our contract design.

Before signing we create a digest of the text as it might be very long.
"""

digest = sha1(b"Something I wish to sign").digest()

print hexlify(digest)

"""Next we can sign it. We need the elliptic curve parameter `G` which we can get from the params we created in the setup. We pass it the public key we generated earlier"""

(G, _, _, _) = params

kinv_rp = do_ecdsa_setup(G, private_key)

"""Now we can sign our digest"""

sig = do_ecdsa_sign(G, private_key, digest, kinv_rp = kinv_rp)

print pack(sig)

"""Finally we can verify it using the public key from our keypair"""

do_ecdsa_verify(G, public_key, sig, digest)

"""Here is a bunch of crypto functions for [homomorphic encryption](https://en.wikipedia.org/wiki/Homomorphic_encryption)

See [here](https://crypto.stackexchange.com/questions/45040/can-elliptic-curve-cryptography-encrypt-with-public-key-and-decrypt-with-private) for an answer about doing encryption with elliptic curve cryptography.

In order to do [asymetric encryption](https://en.wikipedia.org/wiki/Public-key_cryptography)  using ECC you first have to turn your message into a point on the curve,you can then encrypt using a public key which can be decrypted with the private key. 

There is a scheme for more general encrpytion, the https://en.wikipedia.org/wiki/Integrated_Encryption_Scheme which allows two parties to generate a symetric key that they can use to encrypt a message between them
"""


def enc_side(params, pub, counter):
    """ encrypts the values of a small counter """
    assert -2**8 < counter < 2**8
    (_, g, (h0, _, _, _), o) = params

    k = o.random()
    a = k * g
    b = k * pub + counter * h0
    return (a, b, k)


def enc(params, pub, counter):
    """ encrypts the values of a small counter """
    (a, b, k) = enc_side(params, pub, counter)
    return (a, b)


def binencrypt(params, pub, m):
    """ encrypt a binary value m under public key pub """
    assert m in [0, 1]
    return enc_side(params, pub, m)


def add(c1, c2):
    """ add two encrypted counters """
    a1, b1 = c1
    a2, b2 = c2
    return (a1 + a2, b1 + b2)


def add_side(c1, c2, k1, k2):
    """ add two encrypted counters """
    a, b = add(c1, c2)
    return (a, b, k1 + k2)


def sub(c1, c2):
    """ subtract two encrypted counters """
    a1, b1 = c1
    a2, b2 = c2
    return (a1 - a2, b1 - b2)


def sub_side(c1, c2, k1, k2):
    """ subtract two encrypted counters """
    a, b = sub(c1, c2)
    return (a, b, k1 - k2)


def randomize(params, pub, c1):
    """ rerandomize an encrypted counter """
    zero = enc_side(params, pub, 0)
    return add(c1, zero)


def randomize_side(params, pub, c1, k1):
    """ rerandomize an encrypted counter """
    zero_a, zero_b, zero_k = enc_side(params, pub, 0)
    zero_c = (zero_a, zero_b)
    return add_side(c1, zero_c, k1, zero_k)


def make_table(params):
    """ make a decryption table """
    (_, _, (h0, _, _, _), _) = params
    table = {}
    for i in range(-1000, 1000):
        table[i * h0] = i
    return table


def dec(params, table, priv, c1):
    """ Decrypt an encrypted counter """
    a, b = c1
    plain = b + (-priv * a)
    return table[plain]

"""The functions here provide the ability to encrypt a binary value under a public key which can be decrypted using a private key.

Further we can then perform some simple addition using [homomorphic encryption](https://en.wikipedia.org/wiki/Homomorphic_encryption) such that everyone can increment a counter without knowing what the value of it is.

This is the foundation for our voting / petition system. By providing an array of counters representing the options of a vote / petition, voters can choose which one to increment and no-one except the tally counter can see what the results are.
"""

(a, b, k) = binencrypt(params, public_key, 1)

c = (a, b)

print c

encrypted_counter_1 = pack(c)

print encrypted_counter_1

"""And we can decrypt the result using the private key. To do this we need to create a decryption table"""

table  = make_table(params)

decrypted_value = dec(params, table, private_key, unpack(encrypted_counter_1))

print decrypted_value

"""So lets do some homomorphic addition. You first encrypt a new value and then add it to the pervious one."""

(a, b) = enc(params, public_key, 3)
c = (a, b)

new_c = add(unpack(encrypted_counter_1), c)

encrypted_counter_2 = pack(new_c)

print encrypted_counter_2

decrypted_value_2 = dec(params, table, private_key, unpack(encrypted_counter_2))

print "\nResult of addition:"
print decrypted_value_2
