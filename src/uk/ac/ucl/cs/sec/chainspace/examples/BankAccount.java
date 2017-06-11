package uk.ac.ucl.cs.sec.chainspace.examples;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONObject;
import uk.ac.ucl.cs.sec.chainspace.Transaction;

/**
 * This class is an example of chainspace client representing a simple bank transfer. Note that to apply a transaction
 * to the nodes, the transaction has to be in JSON format. Therefore, the clients do not need to be in JAVA.
 */
public class BankAccount {

    // instance variables
    private String accountId;
    private int    amount;

    /**
     * Constructor.
     *
     * @param accountId the acount ID.
     * @param amount the account's balance.
     */
    public BankAccount(String accountId, int amount)
    {
        this.accountId = accountId;
        this.amount    = amount;
    }

    private String getAccountId() {
        return accountId;
    }
    private int getAmount() {
        return amount;
    }

    public Transaction sendMoney(BankAccount account, int amount, String checkerURL) {

        /*
        voluntarily, there a not checks in this method; the amount can
        be negative, etc. We rely on the checker for that (testing purpose)
         */

        // set parameters
        JSONObject json = new JSONObject();
        json.put("amount", amount);
        String parameters = json.toString();

        // set outputs
        BankAccount newSender = new BankAccount(
                this.getAccountId(),
                this.getAmount() - amount
        );
        BankAccount newReceiver = new BankAccount(
                account.getAccountId(),
                account.getAmount() + amount
        );

        Gson gson = new GsonBuilder().create();
        String newSenderJson  = gson.toJson(newSender);
        String newReceiverJson = gson.toJson(newReceiver);

        // set transaction
        return new Transaction(
                checkerURL,
                new int[]{gson.toJson(this).hashCode(), gson.toJson(account).hashCode()},
                new int[]{},
                parameters,
                new String[]{newSenderJson, newReceiverJson}
        );
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BankAccount)) return false;

        BankAccount that = (BankAccount) o;

        return getAccountId().equals(that.getAccountId());
    }

    @Override
    public int hashCode() {
        int result = getAccountId().hashCode();
        result = 31 * result + getAmount();
        return result;
    }
}
