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
	""" generates cryptosystem parameters """
	G = EcGroup()
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



####################################################################
# homomorphic encryptions / decryptions
####################################################################
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


####################################################################
# NIZK proofs
####################################################################
# ------------------------------------------------------------------
# utilities
# ------------------------------------------------------------------
def to_challenge(elements):
    """ generates a Bn challenge by hashing a number of EC points """
    Cstring = b",".join([hexlify(x.export()) for x in elements])
    Chash =  sha256(Cstring).digest()
    return Bn.from_binary(Chash)


# ------------------------------------------------------------------
# proof of bin
# ------------------------------------------------------------------
""" proof """
def provebin(params, pub, ciphertext, k, m):
	""" prove that an encrypted value is binary """

	# unpack the arguments
	(_, g, (h0, h1, _, _), o) = params
	(a, b) = ciphertext

	# create the witnesses
	wk = o.random()
	wm = o.random()

	# compute the witnesses commitments
	Aw = wk * g
	Bw = wk * pub + wm * h0
	Dw = wk * g + (m*(1-m)) * h1

	# create the challenge
	c = to_challenge([g, h0, h1, a, b, Aw, Bw, Dw])

	# create responses for k and m
	rk = (wk - c * k) % o
	rm = (wm - c * m) % o

	# return the proof
	return (c, (rk, rm))

""" verify """
def verifybin(params, pub, ciphertext, proof):
	""" verify proof that an encrypted value is binary """

	# unpack the arguments
	(_, g, (h0, h1, _, _), o) = params
	a, b = ciphertext
	(c, (rk, rm)) = proof

	# re-compute witnesses' commitments
	Aw = c * a + rk * g
	Bw = c * b + rk * pub + rm * h0
	Dw = c * a + rk * g + 0 * h1

	# compute the challenge prime
	c_prime = to_challenge([g, h0, h1, a, b, Aw, Bw, Dw])

	# return whether the proof succeeded
	return c_prime == c


# ------------------------------------------------------------------
# proof of zero
# ------------------------------------------------------------------
""" proof """
def provezero(params, pub, ciphertext, priv):
	""" prove that an encrypted value is zero """

	""" 
	This is essentially as proving the following:
	(1) knowledge of x in (x * k * g)
	indeed:
		cipher = (k * g, k * pub + m * h)

		since m = 0:
		cipher = (k * g, k * pub)
		   	   = (k * g, k * x * g)

		with a = k * g:
		cipher = (a, x * a)

	(2) knowledge of x in (x * g)
	(3) equality of log: x * (k * g) and x * g

	"""

	# unpack the arguments
	(_, g, (h0, _, _, _), o) = params
	(a, b) = ciphertext

	# create the witnesses
	wx = o.random()

	# compute the witnesses' commitments:
	Aw = wx * a 	
	Bw = wx * g 	
		
	# create the challenge
	c = to_challenge([g, h0, pub, a, b, Aw, Bw])

	# create responses for k and m
	rx = (wx - c * priv) % o

	# return the proof
	return (c, rx)

""" verify """
def verifyzero(params, pub, ciphertext, proof):
	""" verify proof that an encrypted value is zero """

	# unpack the arguments
	(_, g, (h0, _, _, _), o) = params
	a, b = ciphertext
	(c, rx) = proof

	# re-compute the witnesses' commitments
	Aw = rx * a + c * b
	Bw = rx * g + c * pub

	# compute the challenge prime
	c_prime = to_challenge([g, h0, pub, a, b, Aw, Bw])

	# return whether the proof succeeded
	return c_prime == c


# ------------------------------------------------------------------
# proof of one
# ------------------------------------------------------------------
""" proof """
def proveone(params, pub, ciphertext, k):
	""" prove that an encrypted value is 1 """

	# unpack the arguments
	(_, g, (h0, _, _, _), o) = params
	(a, b) = ciphertext

	# create the witnesses
	wk = o.random()
	wm = o.random()

	# compute witnesses' commitments
	Aw = wk * g
	Bw = wk * pub + 1 * h0

	# create the challenge
	c = to_challenge([g, h0, pub, a, b, Aw, Bw])

	# create responses for k and m
	rk = (wk - c * k) % o

	# return the proof
	return (c, rk)

""" verify """
def verifyone(params, pub, ciphertext, proof):
	""" verify that an encrypted value is 1 """

	# unpack the arguments
	(_, g, (h0, _, _, _), o) = params
	a, b = ciphertext
	(c, rk) = proof

	# re-compute witnesses' commitments
	Aw = rk * g + c * a
	Bw = rk * pub + c * b + (1 - c) * h0

	# compute the challenge prime
	c_prime = to_challenge([g, h0, pub, a, b, Aw, Bw])

	# return whether the proof succeeded
	return c_prime == c

####################################################################