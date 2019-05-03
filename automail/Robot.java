package automail;

import java.util.Map;
import java.util.TreeMap;

public class Robot {
    protected final String id;
    private MailItem tube = null;
    private int current_floor;
    public enum RobotState { DELIVERING, WAITING, RETURNING };
    private RobotState current_state;
    
    public Robot(){
        id = "R" + hashCode();
        current_floor = Building.MAILROOM_LOCATION;
        current_state = RobotState.RETURNING;
        tube = null;
    }
    
    public boolean isTubeEmpty() {
    	if(tube != null) {
    		return false;
    	}else {
    		return true;
    	}
    }
    
    static private int count = 0;
	static private Map<Integer, Integer> hashMap = new TreeMap<Integer, Integer>();
    
    @Override
	public int hashCode() {
		Integer hash0 = super.hashCode();
		Integer hash = hashMap.get(hash0);
		if (hash == null) { hash = count++; hashMap.put(hash0, hash); }
		return hash;
    }
    
    public MailItem getTube() {
    	return this.tube;
    }

	public void setTube(MailItem mailItem) {
		tube = mailItem;
	}

	public String get_id() {
		return this.id;
	}

	public void set_current_floor(int current_floor) {
		this.current_floor = current_floor;
		
	}

	public void setState(RobotState nextState) {
		this.current_state = nextState;		
	}

	public boolean isEmpty() {
		return this.tube == null;
	}


}
