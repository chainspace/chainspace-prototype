package foo.gettingstarted;

public class RequestType {
    public static final int PUT = 1;
    public static final int GET = 2;
    public static final int REMOVE = 3;
    public static final int SIZE = 4;
    public static final int TRANSACTION = 5;
    public static final String PREPARE_T = "prepare_t";
    public static final String ACCEPT_T_COMMIT = "accept_t_commit";
    public static final String ACCEPT_T_ABORT = "accept_t_abort";
}