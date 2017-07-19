package foo.gettingstarted;

/**
 * Created by sheharbano on 12/07/2017.
 */
public class TransactionSequence {
    boolean PREPARE_T;
    boolean PREPARED_T_ABORT;
    boolean PREPARED_T_COMMIT;
    boolean ACCEPT_T_ABORT;
    boolean ACCEPT_T_COMMIT;
    boolean ACCEPTED_T_ABORT;
    boolean ACCEPTED_T_COMMIT;

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
