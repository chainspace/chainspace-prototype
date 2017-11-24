# Chainspace


## Developer Setup [IntelliJ Ultimate]

There are intellij modules in this folder for each of the submodules. Create a new empty project in intellij. A dialog will prompt you for the project structure, ignore this and wait for the project to load. You will see the .iml module files in the explorer and you can right click and import them to the project from there.

You will need to set the project sdk to be a JDK 1.8 and also a python virtualenv which you can create and link to each python module.

The modules are:

- chainspaceapi [python]
- chainspacecontract [python]
- chainspacemeasurement [python]
- chainspacecore [java]

You will need to add petlib manually to your python virtualenv from a shell... Intellij will have created your virtual env somewhere of your choosing indicated here as  (`$PATH_TO_VIRTUAL_ENV$`)...

```
source $PATH_TO_VIRTUAL_ENV$/bin/activate
pip install petlib
```




