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
# homomorphic encryptions / decryptions
####################################################################
def binencrypt(params, pub, m):
	""" Encrypt a binary value m under public key pub """
	assert m in [0, 1]
	(_, g, (h0, _, _, _), o) = params

	k = o.random()
	a = k * g
	b = k * pub + m * h0
	return (a, b, k)


####################################################################
# NIZK proofs
####################################################################
""" utilities """
def to_challenge(elements):
    """ Generates a Bn challenge by hashing a number of EC points """
    Cstring = b",".join([hexlify(x.export()) for x in elements])
    Chash =  sha256(Cstring).digest()
    return Bn.from_binary(Chash)


""" proofs """
def provezero(params, pub, Ciphertext, k):
	""" prove that an encrypted value is zero """

	# unpack the arguments
	(_, g, (h0, h1, _, _), o) = params
	(a, b) = Ciphertext

	# create the witnesses
	wk = o.random()

	# compute the witnesses' commitments
	Aw = wk * g
	Bw = wk * pub + 0 * h0

	# create the challenge
	c = to_challenge([g, h0, h1, a, b, Aw, Bw])

	# create responses for k and m
	rk = (wk - c * k) % o

	# return the proof
	return (c, rk)

def verifyzero(params, pub, ciphertext, proof):
	""" verify proof that an encrypted value is zero """

	# unpack the arguments
	(_, g, (h0, h1, _, _), o) = params
	a, b = ciphertext
	(c, rk) = proof

	# re-compute the witnesses' commitments
	Aw = c * a + rk * g
	Bw = c * b + rk * pub

	# calculate the challenge prime
	c_prime = to_challenge([g, h0, h1, a, b, Aw, Bw])

	# return whether the proof succeeded
	return c_prime == c


####################################################################