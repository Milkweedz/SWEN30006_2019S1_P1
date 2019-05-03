package automail;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import P1_Automail_V1.SWEN30006_2019S1_P1.swen30006.automail.Building;
import automail.Clock;
import automail.IMailDelivery;
import automail.MailItem;
import strategies.IMailPool;
import strategies.RobotLoader;
import automail.Robot;
import automail.Robot.RobotState;
import exceptions.ExcessiveDeliveryException;
import exceptions.ItemTooHeavyException;

public class RobotTeam {

    static public final int INDIVIDUAL_MAX_WEIGHT = 2000;
    static public final int PAIR_MAX_WEIGHT = 2600;
    static public final int TRIPLE_MAX_WEIGHT = 3000;
    
    static public final float INDIVIDUAL_SPEED = 1;
    static public final float TEAM_SPEED = 1/3;
    

    private Robot[] robots;
    private int teamSize = robots.length;

    IMailDelivery delivery;
    /** Possible states the robot can be in */
    private Robot.RobotState current_state;
    private int current_floor;
    private int destination_floor;
    private IMailPool mailPool;
    private boolean receivedDispatch;
    
    private String id;
    
    private MailItem deliveryItem = null;
    private MailItem tube = null;
    
    private boolean recievedDispatch;
    
    private float max_weight;
    
    private int deliveryCounter;

    private float speed;

    /**
     * Initiates the robot's location at the start to be at the mailroom
     * also set it to be waiting for mail.
     * @param behaviour governs selection of mail items for delivery and behaviour on priority arrivals
     * @param delivery governs the final delivery
     * @param mailPool is the source of mail items
     */
    public RobotTeam(IMailDelivery delivery, IMailPool mailPool, Robot[] robots){
    	id = "T" + hashCode();
        // current_state = TeamState.WAITING;
    	current_state = Robot.RobotState.RETURNING;
        current_floor = Building.MAILROOM_LOCATION;
        this.delivery = delivery;
        this.mailPool = mailPool;
        this.receivedDispatch = false;
        this.deliveryCounter = 0;
        this.robots = robots;
}
    
    public void setTeamSize() {
    	this.teamSize = robots.length;
    }
    
    public void setWeight() {
    	switch(teamSize) {
    		case 1:
    			max_weight = INDIVIDUAL_MAX_WEIGHT;
    		case 2:
    			max_weight = PAIR_MAX_WEIGHT;
    		case 3:
    			max_weight = TRIPLE_MAX_WEIGHT;
    	}
    }
    
    public void setSpeed() {
    	if(teamSize > 1) {
    		this.speed = TEAM_SPEED;
    	}else if(teamSize == 1){
    		this.speed = INDIVIDUAL_SPEED;
    	}
    }
    
    
    public void dispatch() {
    	recievedDispatch = true;
    }

    /**
     * This is called on every time step
     * @throws ExcessiveDeliveryException if robot delivers more than the capacity of the tube without refilling
     */
    public void step() throws ExcessiveDeliveryException{
    	switch(current_state) {
		/** This state is triggered when the robot is returning to the mailroom after a delivery */
		case RETURNING:
			/** If its current position is at the mailroom, then the robot should change state */
            if(current_floor == Building.MAILROOM_LOCATION){
            	if (tube != null) {
            		for(int i; i < teamSize;i++) {
            			mailPool.addToPool(robots[i].getTube());
            			System.out.printf("T: %3d > old addToPool [%s]%n", Clock.Time(), robots[i].getTube().toString());
            			robots[i].setTube(null);
            		}
            	}
    			/** Tell the sorter the robot is ready */
            	for(int i = 0; i < teamSize; i++) {
            		RobotLoader.registerWaiting(robots[i]);
            	}
            	changeState(RobotState.WAITING);
            	for(int i = 0; i< teamSize; i++) {
            		//Need to add taskAssigner class
            		taskAssigner.addWaitingRobots(robots[i]);
            		team.remove(i);
            	}
            	// Need to delete this object in this step or something
            } else {
            	/** If the robot is not at the mailroom floor yet, then move towards it! */
                moveTowards(Building.MAILROOM_LOCATION);
                /* maybe we need this
                if(current_floor == Math.round(current_floor)) {
                }*/
            	break;
            }
		case WAITING:
            /** If the StorageTube is ready and the Robot is waiting in the mailroom then start the delivery */
            if(!isEmpty() && receivedDispatch){
            	receivedDispatch = false;
            	deliveryCounter = 0; // reset delivery counter
    			setRoute();
    			for(int i=0; i < teamSize;i++) {
    				changeState(robots[i],Robot.RobotState.DELIVERING);
    			}
            }
            break;
		case DELIVERING:
			if(current_floor == destination_floor){ // If already here drop off either way
                /** Delivery complete, report this to the simulator! */
                delivery.deliver(deliveryItem);
                deliveryItem = null;
                deliveryCounter++;
                if(deliveryCounter > 2){  // Implies a simulation bug
                	throw new ExcessiveDeliveryException();
                }
                /** Check if want to return, i.e. if there is no item in the tube*/
                for(int i=0; i < teamSize;i++) {
                	if(robots[i].getTube() == null){
                		changeState(robots[i],RobotState.RETURNING);
                		/*Need to assign to add to pool thing 
                		taskAssigner.addTeam(new robotTeam(team.get(i)));
                		*/
                	}else{
                        /** If there is another item, set the robot's route to the location to deliver the item */
                    	setTeamSize();
                        deliveryItem = tube;
                        tube = null;
                        setRoute();
                        for(int j=0; j < teamSize;j++) {
                        	changeState(robots[j],RobotState.DELIVERING);
                        	/*Need to assign to add to pool thing 
                    		taskAssigner.addTeam(new robotTeam(team.get(i)));
                    		*/
                        }
                    }
                }
			} else {
        		/** The robot is not at the destination yet, move towards it! */
                moveTowards(destination_floor);
			}
            break;
    	}
    }
    /**
     * Sets the route for the robot
     */
    private void setRoute() {
    	destination_floor = deliveryItem.getDestFloor();
    }

    /**
     * Generic function that moves the robot towards the destination
     * @param destination the floor towards which the robot is moving
     */
    private void moveTowards(int destination) {
    	if(current_floor < destination){
    		current_floor += speed;
    	} else {
    		current_floor -= speed;
    	}
    	for(int i = 0; i < teamSize; i++) {
    		robots[i].set_current_floor(current_floor);
    	}
    }

    
    //Need to Change
    private String getIdTube() {
    	String robotTeam = "";
    	for(int i = 0; i < teamSize; i++) {
    		robotTeam += robots[i].getTube().toString();
    		robotTeam += " ";
    	}
    	return String.format("%s(%1d)", robots, (tube == null ? 0 : 1));
    }
    
    
    
    /**
     * Prints out the change in state
     * @param robot 
     * @param nextState the state to which the robot is transitioning
     */
    // May need to change
    private void changeState(Robot robot, Robot.RobotState nextState){
    	assert(!(deliveryItem == null && tube != null));
    	if (current_state != nextState) {
    		System.out.printf("T: %3d > %7s changed from %s to %s%n", Clock.Time(), getIdTube(), current_state, nextState);
    	}
    	current_state = nextState;
    	// Changes the state of all the robots for the whole team
    	for(int i = 0; i < teamSize; i++) {
    		robots[i].setState(nextState);
    	}
    	if(nextState == Robot.RobotState.DELIVERING){
    		System.out.printf("T: %3d > %7s-> [%s]%n", Clock.Time(), getIdTube(), deliveryItem.toString());
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

	public boolean isEmpty() {
		return (deliveryItem == null);
	}

	public void addToHand(MailItem mailItem)throws ItemTooHeavyException{
		assert(deliveryItem == null);
		deliveryItem = mailItem;
		if (deliveryItem.getWeight() > max_weight)throw new ItemTooHeavyException();
	}

	public void addToTube(MailItem mailItem) throws ItemTooHeavyException {
		for(int i = 0; i < teamSize; i++) {
			if(robots[i].getTube() == null) {
				robots[i].setTube(mailItem);
				if (tube.weight > max_weight) throw new ItemTooHeavyException();
				break;
			}
		}
	}
}
