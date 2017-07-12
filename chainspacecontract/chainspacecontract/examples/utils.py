""" Some crypto utils based on petlib """

####################################################################
# imports
####################################################################
from hashlib 		import sha256
from binascii 		import hexlify, unhexlify

from petlib.bn 		import Bn
from petlib.ec 		import EcGroup
from petlib.ecdsa   import do_ecdsa_sign, do_ecdsa_verify
from petlib.pack    import encode, decode


####################################################################
# pack / hash
####################################################################
def H(x):
    return hexlify(sha256(x).digest())

def pack(x):
    return hexlify(encode(x))

def unpack(x):
    return decode(unhexlify(x))



####################################################################
# init
####################################################################
def setup(nid=713):
	""" Generates cryptosystem Parameters """
	G = EcGroup()
	g = G.generator()
	hs = [G.hash_to_point(("h%s" % i).encode("utf8")) for i in range(4)]
	o = G.order()
	return (G, g, hs, o)

def key_gen(params):
   """ Generate a private / public key pair """
   (G, g, hs, o) = params
   priv = o.random()
   pub = priv * g
   return (priv, pub)




####################################################################
# NIZK proofs
####################################################################
def to_challenge(elements):
    """ Generates a Bn challenge by hashing a number of EC points """
    Cstring = b",".join([hexlify(x.export()) for x in elements])
    Chash =  sha256(Cstring).digest()
    return Bn.from_binary(Chash)



####################################################################