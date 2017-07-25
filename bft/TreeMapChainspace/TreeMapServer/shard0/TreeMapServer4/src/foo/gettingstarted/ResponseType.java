package foo.gettingstarted;

/**
 * Created by sheharbano on 12/07/2017.
 */

public class ResponseType {
    // Replica-to-Replica replies
    public static final String PREPARED_T_COMMIT = "prepared_t_commit";
    public static final String PREPARED_T_ABORT = "prepared_t_abort";
    public static final String ACCEPTED_T_COMMIT = "accepted_t_commit";
    public static final String ACCEPTED_T_ABORT = "accepted_t_abort";
    // Replica-to-client replies
    public static final String PREPARE_T_SYSTEM_ERROR = "PREPARE_T_SYSTEM_ERROR";
    public static final String ACCEPT_T_SYSTEM_ERROR = "ACCEPT_T_SYSTEM_ERROR";
    public static final String SUBMIT_T_SYSTEM_ERROR = "SUBMIT_T_SYSTEM_ERROR";
    public static final String SUBMIT_T_UNKNOWN_REQUEST = "SUBMIT_T_UNKNOWN_REQUEST";
    public static final String PREPARED_T_ABORT_NOOBJECT = "PREPARED_T_ABORT_NOOBJECT";
    public static final String PREPARED_T_ABORT_LOCKEDOBJECT = "PREPARED_T_ABORT_LOCKEDOBJECT";
    public static final String PREPARED_T_ABORT_INACTIVEOBJECT = "PREPARED_T_ABORT_INACTIVEOBJECT";
    public static final String PREPARED_T_ABORT_BADTRANSACTION = "PREPARED_T_ABORT_BADTRANSACTION";
    public static final String ACCEPTED_T_ABORT_INTRASHARD = "ACCEPTED_T_ABORT_INTRASHARD";
    public static final String CREATED_OBJECT = "CREATED_OBJECT";
    public static final String CREATE_OBJECT_SYSTEM_ERROR = "CREATE_OBJECT_SYSTEM_ERROR";
}
