package automail;

import exceptions.ExcessiveDeliveryException;
import exceptions.ItemTooHeavyException;
import strategies.IMailPool;
import strategies.MailPool;

import javax.print.attribute.standard.Destination;

import java.util.ArrayList;
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
    public IMailPool mailPool;
    public ArrayList<Robot> robots;

    private MailItem deliveryItem = null;
    public int max_weight;
    
    private int deliveryCounter;
	private int speed;
    

    /**
     * Initiates the robot's location at the start to be at the mailroom
     * also set it to be waiting for mail.
     * #param behaviour governs selection of mail items for delivery and behaviour on priority arrivals
     * @param mailPool 
     * @param delivery governs the final delivery
     * @param robotLoader2 is the controller and mail loader of robots
     */

	public RobotTeam(IMailPool mailPool, IMailDelivery delivery, RobotLoader robotLoader, ArrayList<Robot> teamMembers) {
		id = "T" + hashCode();
        // current_state = TeamState.WAITING;
    	current_state = TeamState.RETURNING;
        current_floor = Building.MAILROOM_LOCATION;
        this.delivery = delivery;
        this.mailPool = mailPool;
        this.robotLoader = robotLoader;
        this.receivedDispatch = false;
        this.deliveryCounter = 0;
        this.robots = teamMembers;
	}
	public RobotTeam createNewRobotTeam(Robot robot, RobotTeam currentTeam) {
		ArrayList<Robot> new_robot_array = new ArrayList<>();
		new_robot_array.add(robot);
		RobotTeam new_robot_team = new RobotTeam(mailPool, delivery, robotLoader, new_robot_array);
		return new_robot_team;
	}
	
	public void setMaxWeight() {
		if(robots.size() == 1) {
			max_weight = INDIVIDUAL_MAX_WEIGHT;
		}else if(robots.size() == 2) {
			max_weight = PAIR_MAX_WEIGHT;
		}else if(robots.size() == 3) {
			max_weight = TRIPLE_MAX_WEIGHT;
		}
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
                    if (isEmpty()) {
                    	System.out.printf("adding team "+ id);
                    	System.out.println(" back to pool");
                    	System.out.println(robots.size() + " robots added back to pool");
                    	robotLoader.registerWaiting(this);
                    	RobotLoader.robotTeamList.remove(this);
                    }
        			/** Tell the sorter the robot is ready */
        			
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
                	//System.out.println(current_state);
                }
                break;
    		case DELIVERING:
    			if(current_floor == destination_floor){ // If already here drop off either way
    				System.out.println(id +" has successfully delivered item!");
                    /** Delivery complete, report this to the simulator! */
                    delivery.deliver(deliveryItem);
                    deliveryItem = null;
                    deliveryCounter++;
                    //System.out.println(deliveryCounter);
                    if(deliveryCounter > 2){  // Implies a simulation bug
                    	throw new ExcessiveDeliveryException();
                    }

                    for(int i = 1; i < robots.size(); i++) {
                    	if(isTubeEmpty(robots.get(i))) {
                    		RobotTeam returning_Team = createNewRobotTeam(robots.get(i),this);
                    		returning_Team.setSpeed();
                    		returning_Team.changeState(TeamState.RETURNING);
                    		System.out.println("Team is Splitting and Returning");
                    		System.out.println("New Split Team ID: " + returning_Team.id);
                    		RobotLoader.robotTeamList.add(returning_Team);
                    	}else {
                    		RobotTeam temp_robot = createNewRobotTeam(robots.get(i),this);
                    		temp_robot.moveToHand();
                    		temp_robot.setSpeed();
                    		temp_robot.changeState(TeamState.DELIVERING);
                    		System.out.println("Team is Splitting and Delivering again");
                    		System.out.println("New Split Team ID: " + temp_robot.id);
                    		RobotLoader.robotTeamList.add(temp_robot);
                    	}
                    	robots.remove(i);
                    }
                    
                    if(isTubeEmpty(robots.get(0))) {
                    	setSpeed();
                    	changeState(TeamState.RETURNING);
                    }else {
                    	moveToHand();
                    	setRoute();
                    	setSpeed();
                    }
    			} else {
	        		/** The robot is not at the destination yet, move towards it! */
    				//System.out.println("moving");
	                moveTowards(destination_floor);
    			}
                break;
    	}
    }

	public void setSpeed() {
		if(robots.size() > 1) {
			this.speed = 3;
		}else {
			this.speed = 1;
		}
		
	}
	/**
	 * Move mail item from tube to hand
	 */
	public void tubeToHand(){
		assert(robots.size() == 1);
		deliveryItem = robots.get(0).getTube();
		robots.get(0).emptyTube();
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
    	int[] itemsHeld = new int[robots.size()];
    	int num_itemsHeld = 0;
    	String robotAndItems = "";
    	for (int i=0; i< robots.size(); i++){
			num_itemsHeld += ((robots.get(i).getTube() == null) ? 0 : 1);
		}
    	
		robotAndItems = robotAndItems.concat(String.format("%s(%1d), ", id, num_itemsHeld));

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
			if (robot.getTube() != null){
				tubeFull = true;
			}
		}
		return tubeFull;
	}

	private boolean isTubeEmpty(Robot robot) {
    	return robot.getTube()==null;
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
        return deliveryItem == null && robots.get(0).getTube() == null;
	}

	public void addToHand(MailItem mailItem) throws ItemTooHeavyException {
		assert(deliveryItem == null);
		setMaxWeight();
		deliveryItem = mailItem;
		if (deliveryItem.weight > max_weight) throw new ItemTooHeavyException();
	}
	
	public void moveToHand() {
		deliveryItem = robots.get(0).getTube();
		robots.get(0).emptyTube();
	}


}
