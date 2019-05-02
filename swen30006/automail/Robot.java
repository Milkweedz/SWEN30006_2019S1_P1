package automail;

public class Robot {
    protected final String id;
    protected MailItem tube = null;

    public Robot(){
        id = "R" + hashCode();
    }


}
