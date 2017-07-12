package foo.gettingstarted;

/**
 * Created by sheharbano on 12/07/2017.
 */

public class ResponseType {
    // Replica-to-Replica replies
    public static final String PREPARED_T_COMMIT = "prepared_t_commit";
    public static final String ACCEPTED_T_COMMIT = "accepted_t_commit";
    public static final String ACCEPTED_T_ABORT = "accepted_t_commit";
    // Replica-to-client replies
    public static final String PREPARED_T_ABORT_NOOBJECT = "PREPARED_T_ABORT_NOOBJECT";
    public static final String PREPARED_T_ABORT_LOCKEDOBJECT = "PREPARED_T_ABORT_LOCKEDOBJECT";
    public static final String PREPARED_T_ABORT_INACTIVEOBJECT = "PREPARED_T_ABORT_INACTIVEOBJECT";
    public static final String PREPARED_T_ABORT_BADTRANSACTION = "PREPARED_T_ABORT_BADTRANSACTION";
    public static final String ACCEPTED_T_ABORT_INTRASHARD = "ACCEPTED_T_ABORT_INTRASHARD";
}
