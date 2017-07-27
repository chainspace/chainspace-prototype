package foo.gettingstarted;

/**
 * TransactionExtended
 * Extends Transaction to add a JSON representation of a Chainspace's transaction.
 */
public class TransactionExtended extends Transaction
{
	// instance variables
    private String chainspaceTransactionJson;

    /**
     * constructor
     * Saves a JSON representation of a Chainspace's transaction.
     * @param chainspaceTransactionJson 	JSON representation of a Chainspace's transaction
     */
    public TransactionExtended(String chainspaceTransactionJson) {

        this.chainspaceTransactionJson = chainspaceTransactionJson;

        this.addInput("2");
        this.addOutput("4");

    }

    /**
     * getChainspaceTransactionJson
     * Get the Chainspace's JSON transaction
     * @return the Chainspace's JSON transaction
     */
    public String getChainspaceTransactionJson() {

        return this.chainspaceTransactionJson;

    }
}