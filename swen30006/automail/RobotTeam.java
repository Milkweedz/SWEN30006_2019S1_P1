package automail;

import exceptions.ExcessiveDeliveryException;
import exceptions.ItemTooHeavyException;
import strategies.IMailPool;

import javax.print.attribute.standard.Destination;
import java.util.Map;
import java.util.TreeMap;

/**
 * The robot team delivers mail!
 */
public class RobotTeam {
	
    static public final int INDIVIDUAL_MAX_WEIGHT = 2000;
    static public final int PAIR_MAX_WEIGHT = 2600;
    static public final int TRIPLE_MAX_WEIGHT = 3000;

    IMailDelivery delivery;
    protected final String id;
    /** Possible states the robot can be in */
    public enum TeamState { DELIVERING, WAITING, RETURNING }
    public TeamState current_state;
    private int current_floor;
    private int destination_floor;
    private RobotLoader robotLoader;
    private boolean receivedDispatch;

    public Robot[] robots;

    private MailItem deliveryItem = null;
    public int TEAM_MAX_WEIGHT;
    
    private int deliveryCounter;
    

    /**
     * Initiates the robot's location at the start to be at the mailroom
     * also set it to be waiting for mail.
     * #param behaviour governs selection of mail items for delivery and behaviour on priority arrivals
     * @param delivery governs the final delivery
     * @param robotLoader is the controller and mail loader of robots
     */
    public RobotTeam(IMailDelivery delivery, RobotLoader robotLoader, Robot[] robots){
    	id = "T" + hashCode();
        // current_state = TeamState.WAITING;
    	current_state = TeamState.RETURNING;
        current_floor = Building.MAILROOM_LOCATION;
        this.delivery = delivery;
        this.robotLoader = robotLoader;
        this.receivedDispatch = false;
        this.deliveryCounter = 0;
        this.robots = robots;
    }
    
    public void dispatch() {
    	receivedDispatch = true;
    }

    /**
     * This is called on every time step
     * @throws ExcessiveDeliveryException if robot delivers more than the capacity of the tube without refilling
     */
    public void step() throws ExcessiveDeliveryException {    	
    	switch(current_state) {
    		/** This state is triggered when the robot is returning to the mailroom after a delivery */
    		case RETURNING:
    			/** If its current position is at the mailroom, then the robot should change state */
                if(current_floor == Building.MAILROOM_LOCATION){
                    for (Robot robot : robots) {
                        if (robot.tube != null) {
//                            mailPool.addToPool(robot.tube);
//                            System.out.printf("T: %3d > old addToPool [%s]%n", Clock.Time(), tube.toString());
                            robot.tube = null;
                        }
                    }
        			/** Tell the sorter the robot is ready */
        			robotLoader.registerWaiting(this);
                	changeState(TeamState.WAITING);
                } else {
                	/** If the robot is not at the mailroom floor yet, then move towards it! */
                    moveTowards(Building.MAILROOM_LOCATION);
                	break;
                }
    		case WAITING:
                /** If the StorageTube is ready and the Robot is waiting in the mailroom then start the delivery */
                if(!isEmpty() && receivedDispatch){
                	receivedDispatch = false;
                	deliveryCounter = 0; // reset delivery counter
        			setRoute();
                	changeState(TeamState.DELIVERING);
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

                    if (tubeEmpty()){
                    	changeState(TeamState.RETURNING);
					}
					else {
						robotLoader.registerFinished(this);
					}

                    /** Check if want to return, i.e. if there is no item in the tube*/
//                    if(tube == null){
//                    	changeState(TeamState.RETURNING);
//                    }
//                    else{
//                        /** If there is another item, set the robot's route to the location to deliver the item */
//                        deliveryItem = tube;
//                        tube = null;
//                        setRoute();
//                        changeState(TeamState.DELIVERING);
//                    }
    			} else {
	        		/** The robot is not at the destination yet, move towards it! */
	                moveTowards(destination_floor);
    			}
                break;
    	}
    }

	/**
	 * Move mail item from tube to hand
	 */
	public void tubeToHand(){
		assert(robots.length == 1);
		deliveryItem = robots[0].tube;
		robots[0].tube = null;
		destination_floor = deliveryItem.getDestFloor();
		setRoute();
		changeState(TeamState.DELIVERING);
	}

	/**
     * Sets the route for the robot
     */
    private void setRoute() {
        /** Set the destination floor */
        destination_floor = deliveryItem.getDestFloor();
    }

    /**
     * Generic function that moves the robot towards the destination
     * @param destination the floor towards which the robot is moving
     */
    private void moveTowards(int destination) {
        if(current_floor < destination){
            current_floor++;
        } else {
            current_floor--;
        }
    }
    
    private String getIdRobot() {
    	String[] ids = new String[robots.length];
    	int[] itemsHeld = new int[robots.length];
    	String robotAndItems = "";
    	for (int i=0; i< robots.length; i++){
    		ids[i] = robots[i].id;
			itemsHeld[i] = (robots[i].tube == null) ? 0 : 1;
		}
    	for (int i=0; i<ids.length;i++){
			robotAndItems = robotAndItems.concat(String.format("%s(%1d), ", ids[i], itemsHeld[i]));
		}

		return robotAndItems;
    }
    
    /**
     * Prints out the change in state
     * @param nextState the state to which the robot is transitioning
     */
    private void changeState(TeamState nextState){
    	assert(!(deliveryItem == null && tubeFull()));
    	if (current_state != nextState) {
            System.out.printf("T: %3d > %7s changed from %s to %s%n", Clock.Time(), getIdRobot(), current_state, nextState);
    	}
    	current_state = nextState;
    	if(nextState == TeamState.DELIVERING){
            System.out.printf("T: %3d > %7s-> [%s]%n", Clock.Time(), getIdRobot(), deliveryItem.toString());
    	}
    }

	private boolean tubeFull() {
    	boolean tubeFull = false;
		for (Robot robot : robots){
			if (robot.tube != null){
				tubeFull = true;
			}
		}
		return tubeFull;
	}

	private boolean tubeEmpty() {
    	boolean tubeEmpty = true;
    	for (Robot robot : robots){
    		if (robot.tube != null){
    			tubeEmpty = false;
			}
		}
		return tubeEmpty;
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
	    if (deliveryItem != null) { return false; }
	    for (Robot robot : robots) {
            if (robot.tube != null) { return false; }
        }
        return true;
	}

	public void addToHand(MailItem mailItem) throws ItemTooHeavyException {
		assert(deliveryItem == null);
		deliveryItem = mailItem;
		if (deliveryItem.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
	}


}
