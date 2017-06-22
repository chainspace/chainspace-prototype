package uk.ac.ucl.cs.sec.chainspace;

import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;

/**
 *
 *
 * This is a separate static class, so if we decide to change the transaction's format in the future (change the
 * key-value store, use tree structure, etc), the core is not modified: the core only requires the original transaction
 * (the one with the ID) and the transaction to send to the checker.
 *
 */
class TransactionPackager {

    /**
     * makeTransaction
     * Extract a transaction object from the json request.
     */
    static Transaction makeTransaction(String request) throws AbortTransactionException {
        // get json request
        JSONObject requestJson = new JSONObject(request);

        // get the transaction and the key-value store as java objects
        Transaction transaction;
        try {

            // create java objects
            transaction = Transaction.fromJson(requestJson.getJSONObject("transaction"));

        }
        catch (Exception e) {
            throw new AbortTransactionException("Malformed transaction.");
        }

        // return transaction
        return transaction;
    }


    /**
     * makeFullTransaction
     * convert a key-value store and a transaction into a fullTransaction java object
     */
    static TransactionForChecker makeFullTransaction(String request) throws AbortTransactionException {

        // get json request
        JSONObject requestJson = new JSONObject(request);

        // get the transaction and the key-value store as java objects
        Transaction transaction;
        Store store;
        try {

            // create java objects
            transaction = Transaction.fromJson(requestJson.getJSONObject("transaction"));
            store = Store.fromJson(requestJson.getJSONArray("store"));

            // check transaction's integrity
            if (! checkTransactionIntegrity(transaction, store)) {
                throw new Exception();
            }

        }
        catch (Exception e) {
            throw new AbortTransactionException("Malformed transaction or key-value store.");
        }

        // assemble inputs objects
        String[] inputs = new String[transaction.getInputIDs().length];
        for (int i = 0; i < transaction.getInputIDs().length; i++) {
            inputs[i] = store.getValueFromKey(transaction.getInputIDs()[i]);
        }

        // assemble reference inputs objects
        String[] referenceInputs = new String[transaction.getReferenceInputIDs().length];
        for (int i = 0; i < transaction.getReferenceInputIDs().length; i++) {
            referenceInputs[i] = store.getValueFromKey(transaction.getReferenceInputIDs()[i]);
        }

        // assemble output objects
        String[] outputs = new String[transaction.getOutputIDs().length];
        for (int i = 0; i < transaction.getOutputIDs().length; i++) {
            outputs[i] = store.getValueFromKey(transaction.getOutputIDs()[i]);
        }

        // create full transaction
        return new TransactionForChecker(
                transaction.getContractID(),
                inputs,
                referenceInputs,
                transaction.getParameters().clone(),
                transaction.getReturns().clone(),
                outputs,
                transaction.getDependencies()
        );

    }

    /**
     * checkTransactionIntegrity
     * Check the transaction's integrity.
     */
    private static boolean checkTransactionIntegrity(Transaction transaction, Store store)
            throws NoSuchAlgorithmException
    {

        // check transaction's and store's format
        // all fields must be present. For instance, if a transaction has no parameters, and empty field should be sent
        if (store.getArray() == null
            || transaction.getInputIDs() == null
            || transaction.getReferenceInputIDs() == null
            || transaction.getReturns() == null
            || transaction.getParameters() == null
            || transaction.getOutputIDs() == null
            || transaction.getDependencies() == null )
        {
            return false;
        }


        /*
        TODO: for input objects, check that the ID in the transaction and in the store matche the value in the db.
            in other words:

            FOR (every transaction's input ID)
                vFromStore  = getValueFromStore(ID)
                valueFromDB = getObjectFromDB(ID)
                IF ( hash(vFromStore) == hash(valueFromDB) OR valueFromDB == NOT_FOUND )
                    OK
                ELSE
                    KO


        // check hashed of input objects
        for (String inputID: transaction.getInputIDs()) {
            if (! Utils.verifyHash(store.getValueFromKey(inputID), inputID)) {
                return false;
            }
        }

        // check hashed of reference input objects
        for (String referenceInputID: transaction.getReferenceInputIDs()) {
            if (! Utils.verifyHash(store.getValueFromKey(referenceInputID), referenceInputID)) {
                return false;
            }
        }
        */


        // otherwise, return true
        return true;

    }

}
