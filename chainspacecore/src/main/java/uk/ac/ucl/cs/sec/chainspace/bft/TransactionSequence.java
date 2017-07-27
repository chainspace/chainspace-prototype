package uk.ac.ucl.cs.sec.chainspace.bft;

/**
 * Created by sheharbano on 12/07/2017.
 */
public class TransactionSequence {
    public boolean PREPARE_T;
    public boolean PREPARED_T_ABORT;
    public boolean PREPARED_T_COMMIT;
    public boolean ACCEPT_T_ABORT;
    public boolean ACCEPT_T_COMMIT;
    public boolean ACCEPTED_T_ABORT;
    public boolean ACCEPTED_T_COMMIT;

    public TransactionSequence() {
        PREPARE_T = false;
        PREPARED_T_ABORT = false;
        PREPARED_T_COMMIT = false;
        ACCEPT_T_ABORT = false;
        ACCEPT_T_COMMIT = false;
        ACCEPTED_T_ABORT = false;
        ACCEPTED_T_COMMIT = false;
    }
}
