package uk.ac.ucl.cs.sec.chainspace;


import java.security.NoSuchAlgorithmException;

/**
 * TransactionPackager
 *
 */
public class TransactionPackager {


    /**
     * makeFullTransaction
     * convert a key-value store and a transaction into a fullTransaction java object
     */
    public static TransactionForChecker makeTransactionForChecker(CSTransaction transaction, Store store)
            throws AbortTransactionException, NoSuchAlgorithmException
    {
        // check transaction's integrity
        if (! checkTransactionIntegrity(transaction, store)) {
            throw new AbortTransactionException ("Malformed transaction or id-value store.");
        }

        // assemble inputs objects
        String[] inputs = new String[transaction.getInputIDs().length];
        for (int i = 0; i < transaction.getInputIDs().length; i++) {
            inputs[i] = store.get(transaction.getInputIDs()[i]);
        }

        // assemble reference inputs objects
        String[] referenceInputs = new String[transaction.getReferenceInputIDs().length];
        for (int i = 0; i < transaction.getReferenceInputIDs().length; i++) {
            referenceInputs[i] = store.get(transaction.getReferenceInputIDs()[i]);
        }

        // create transaction for checker
        return new TransactionForChecker(
            transaction.getContractID(),
            inputs,
            referenceInputs,
            transaction.getParameters().clone(),
            transaction.getReturns().clone(),
            transaction.getOutputs().clone(),
            CSTransaction.toStringArray(transaction.getDependencies()),
            transaction.getMethodID()
        );

    }


    /**
     * checkTransactionIntegrity
     * Check the transaction's integrity.
     */
    private static boolean checkTransactionIntegrity(CSTransaction transaction, Store store)
            throws NoSuchAlgorithmException
    {
        // check transaction's and store's format
        // all fields must be present. For instance, if a transaction has no parameters, an empty array should be sent
        if ( store == null
            || transaction.getInputIDs()          == null
            || transaction.getReferenceInputIDs() == null
            || transaction.getReturns()           == null
            || transaction.getParameters()        == null
            || transaction.getOutputs()           == null
            || transaction.getDependencies()      == null
        ){
            return false;
        }

        /*
        TODO: for input and reference input objects, check that the ID in the store matche the value in the db.

            FOR (every transaction's input ID)
                valueFromStore  = getValueFromStore(ID)
                valueFromDB     = getObjectFromDB(ID)
                IF ( hash(valueFromStore) == hash(valueFromDB) OR valueFromDB == NOT_FOUND )
                    RETURN TRUE
                ELSE
                    RETURN FALSE

        */

        // otherwise
        return true;

    }

}
