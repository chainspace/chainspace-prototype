package uk.ac.ucl.cs.sec.chainspace.bft;

import java.util.HashMap;

public class RequestType {

    public static final int PUT = 1;
    public static final int GET = 2;
    public static final int REMOVE = 3;
    public static final int SIZE = 4;
    public static final int TRANSACTION_VALIDITY = 5;
    public static final int TRANSACTION_SUBMIT = 6;  // The ChainSpace client sends this request to shard(s)
    // The requests below are sent by a BFT leader
    public static final int PREPARE_T = 7;
    public static final int ACCEPT_T_ABORT = 8;
    public static final int ACCEPT_T_COMMIT = 9;
    // The requests below are sent via broadcast, and are just meant to
    // inform replicas to update their state
    public static final int CREATE_OBJECT = 10;
    public static final int PREPARED_T_COMMIT = 14;
    public static final int PREPARED_T_ABORT = 15;
    public static final int ACCEPTED_T_COMMIT = 16;
    public static final int ACCEPTED_T_ABORT = 17;;

    public static String getReqName(int n) {
        String name = "";
        switch(n) {
            case 1:
                name = "PUT";
                break;
            case 2:
                name = "GET";
                break;
            case 3:
                name = "REMOVE";
                break;
            case 4:
                name = "SIZE";
                break;
            case 5:
                name = "TRANSACTION_VALIDITY";
                break;
            case 6:
                name = "TRANSACTION_SUBMIT";
                break;
            case 7:
                name = "PREPARE_T";
                break;
            case 8:
                name = "ACCEPT_T_ABORT";
                break;
            case 9:
                name = "ACCEPT_T_COMMIT";
                break;
            case 10:
                name = "CREATE_OBJECT";
                break;
            case 14:
                name = "PREPARED_T_COMMIT";
                break;
            case 15:
                name = "PREPARED_T_ABORT";
                break;
            case 16:
                name = "ACCEPTED_T_COMMIT";
                break;
            case 17:
                name = "ACCEPTED_T_ABORT";
                break;
            default:
                name = "UNKNOWN";
        }
        return name;
    }
}