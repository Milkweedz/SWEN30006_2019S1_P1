package automail;

public class Robot {
    protected final String id;
    private MailItem tube = null;

    public Robot(){
        id = "R" + hashCode();
    }

	public void addToTube(MailItem mailItem){
		assert(tube == null);
		tube = mailItem;
	}
	public MailItem getTube() {
		return this.tube;
	}
	public void emptyTube() {
		this.tube = null;
	}
}
