.. chainspacecontract documentation master file, created by
   sphinx-quickstart on Fri Dec 22 11:29:16 2017.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

Welcome to chainspacecontract's documentation!
==============================================

.. toctree::
   :maxdepth: 3
   :caption: Contents:


The Chainspace architecture
---------------------------

Chainspace is a distributed computation platform (a *Distributed Ledger*). *Smart Contracts* are user defined. They create and manage contract state, in the form of *objects*. Chainspace is designed to allow for scalability, by allowing objects to be stored and processed across multiple shards. However, this imposes some constraints on the definition of contracts, which the *chainspacecontract* python library abstracts.

A smart contract is defined by three components:

Init
  An **init** function that is executed upon initializing the contract, that creates a set of 
  initial objects for the contract.

Methods
  One or more **methods** (also called *procedures*). Each takes a set of contract objects as 
  inputs or references, as well as other helper variables, and output contract objects and other information. A method may be an arbitrary piece of code that performs a computation.

Checkers
  Each method is paired with a **checker** function. The checker takes inputs, references, and 
  outputs, and public local variables input and output from the corresponding method, and returns True or False. Checkers must be **deterministic and side-effect free** (pure functions).

A contract is correct if all inputs and outputs of valid invocations of methods, return True when passed to the corresponding checker. The checker must return False for invocations that violate the contract invariants. 

Checkers are loaded as contracts in the Chainspace infrastructure, for clients to use them to process contract objects. Clients use the methods to manipulate objects, and construct transactions -- and submit those to the chainspace infrastructure for processing. The infrastructure commits or aborts transactions depending on whether they are valid, according to the contract checkers, and also according to whether the objects they use as inputs and references are unused. 

All inputs to a committed transactions become 'used', and cannot be reused again in a new transactions. This is the key mechanism to prevent 'double spending'. Chainspace ensures that at most one of two transactions unsing the same input commits, and the other aborts.

More information on the Chainspace system can be found in the Chainspace technical paper:

* Chainspace: A Sharded Smart Contracts Platform.
  Mustafa Al-Bassam, Alberto Sonnino, Shehar Bano, Dave Hrycyszyn and George Danezis. NDSS 2018.
  https://arxiv.org/pdf/1708.03778.pdf

Tutorial: A first Chainspace smart contract
-------------------------------------------

The ``chainspacecontract`` framework provides a number of helper functions and classes to define contracts. A new smart contract is first defined using the ``ChainspaceContract`` constructor. 

.. literalinclude:: ../chainspacecontract/examples/hello.py
   :language: python
   :lines: 9-12
   :linenos:
   :caption: hello.py

All contracts must define an ``init`` method, that returns a set of initial objects. Those are delivered in a dictionary keyed by ``output``. Note that all output objects need to be strings, and we use ``json`` to marshal them.

This returns an object that can be used to define methods and checkers. An ``init`` method has no checker, and takes no arguments. Yet is is decorated with the ``@contract.method('init')`` decorator.

.. literalinclude:: ../chainspacecontract/examples/hello.py
   :language: python
   :pyobject: init 
   :linenos:  
   :caption: hello.py

We then define an actual procedure called *hello*, through the decorator ``@contract.method('hello')``. This simply takes the contract token (output by the init function), and creates a new object of type *HelloMessage*, as well as a new copy of the token. Note that methods that create output objects must always consume at least one input object, and the contract token pattern is a common way to satisfy this requirement.

.. literalinclude:: ../chainspacecontract/examples/hello.py
   :language: python
   :pyobject: hello   
   :linenos:
   :caption: hello.py

Note that *hello* can be an arbitrary function, as long as its first three arguments correspond to lists of inputs, references and public parameters. Other parameters are considered secrets, and will not be used to check the method. The output of a method is a dictionary that contains a tuple of output objects under 'outputs', and optionally a tuple of public 'returns'.

The checker for *hello* is defined through the decorator ``@contract.checker('hello')``. Checkers take as parameters a sequence of tuples *inputs, reference_inputs, parameters, outputs, returns, dependencies*. Inputs and references are the same as those passed to the corresponding method. The *parameters* tuple contains all public parameters and returns. The *outputs* are the sequence of objects returned by the method. Dependencies are an advanced concept allowing for contract composition. A checker must always return a boolean.

.. literalinclude:: ../chainspacecontract/examples/hello.py
   :language: python
   :pyobject: hello_checker
   :linenos:
   :caption: hello.py

The ``chainspacecontract`` framework provides facilities for testing contracts. We recommend the use of the ``pytest`` testing library, but the facilities should work with other frameworks. 

Testing code should import the contract and helper functions, as well as a library to do HTTP posts. We use here the ``requests`` library:

.. literalinclude:: ../chainspacecontract/test/test_hello.py
   :language: python
   :linenos:
   :lines: 8-14
   :caption: test_hello.py

Each contract provides a context manager, accessed through ``.test_service()``. Internally it spins a webserver (using ``flask``) listening on port 5000, for requests to the checker. This expects a JSON encoded POST to the endpoint corresponding to the method to be checked. The transaction needs to be encoded via the ``transaction_to_solution`` helper function.

We can test the ``init`` method first -- this simply executes it.

.. literalinclude:: ../chainspacecontract/test/test_hello.py
   :language: python
   :linenos:
   :pyobject: test_init
   :caption: test_hello.py

A more meaningful test involves the checker for *hello*. The method may be invoked to create a transaction, and then the transaction is packaged and sent to the web checker as a JSON POST. As expected the response reports "success".

.. literalinclude:: ../chainspacecontract/test/test_hello.py
   :language: python
   :linenos:
   :pyobject: test_hello
   :caption: test_hello.py


This concludes the tutorial, however there are a number of advanced features we did not cover:

 * Cross contract calls, dependent transactions and callbacks.
 * Trivial checkers.
 * Performance tips, and distribution for checkers.
 * How to upload the contracts into Chainspace, and run them.

API Documentation
-----------------

.. autoclass:: chainspacecontract.ChainspaceContract
   :members:


.. autofunction:: chainspacecontract.transaction_inline_objects


Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
* :ref:`search`
