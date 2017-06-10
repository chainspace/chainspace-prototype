package uk.ac.ucl.cs.sec.chainspace.examples;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.simple.JSONObject;
import uk.ac.ucl.cs.sec.chainspace.ChainspaceObject;
import uk.ac.ucl.cs.sec.chainspace.Transaction;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Created by alberto on 09/06/2017.
 */
public class BankAccount {

    // instance variables
    private String accountId;
    private int amount;

    /**
     * Constructor.
     *
     * @param accountId the acount ID.
     * @param amount the account's balance.
     */
    public BankAccount(String accountId, int amount)
    {
        this.accountId = accountId;
        this.amount = amount;
    }

    public String getAccountId() {
        return accountId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public Transaction sendMoney(BankAccount account, int amount, String checkerURL) {
        /*
        voluntarily, there a not checks in this method; the amount can
        be negative, etc. We rely on the checker for that (testing purpose)
         */

        // set parameters
        JSONObject json = new JSONObject();
        StringWriter out = new StringWriter();
        json.put("amount", amount);
        try { json.writeJSONString(out); }
        catch (IOException e) { e.printStackTrace(); }
        String parameters = out.toString();

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
