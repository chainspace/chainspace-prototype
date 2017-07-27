package foo.gettingstarted;

/**
 * Created by sheharbano on 12/07/2017.
 */
public class ObjectStatus {
    public static final String ACTIVE = "0";
    public static final String LOCKED = "1";
    public static final String INACTIVE = "2";

    public static int mapObjectToShard(String object) {
        int iObject = Integer.parseInt(object);
        if(iObject%2 == 0)
            return 0;
        else
            return 1;
    }
}
