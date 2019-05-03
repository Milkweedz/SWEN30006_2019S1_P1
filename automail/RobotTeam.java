package automail;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import automail.Clock;
import automail.IMailDelivery;
import automail.MailItem;
import strategies.Automail;
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
    

    private ArrayList<Robot> robots = new ArrayList<>();
    private int teamSize = robots.size();

    IMailDelivery delivery;
    /** Possible states the robot can be in */
    private RobotState current_state;
    private int current_floor;
    private int destination_floor;
    private IMailPool mailPool;
    private boolean receivedDispatch;
    private RobotLoader robotloader;
    
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
    public RobotTeam(IMailDelivery delivery, IMailPool mailPool, RobotLoader robotloader, ArrayList<Robot> robots){
    	id = "T" + hashCode();
        // current state should always be delivering as
    	// RobotTeam is only made when delivery is needed
    	current_state = RobotState.DELIVERING;
        current_floor = Building.MAILROOM_LOCATION;
        this.delivery = delivery;
        this.mailPool = mailPool;
        // Should always be true
        this.receivedDispatch = true;
        this.deliveryCounter = 0;
        this.robots = robots;
        this.robotloader = robotloader;
	this.setWeight();
        this.setSpeed();
    }
    
    public RobotTeam createNewRobotTeam(IMailDelivery delivery, IMailPool mailPool, RobotLoader robotloader, ArrayList<Robot> robots){
    	RobotTeam new_RobotTeam = new RobotTeam(delivery, mailPool, robotloader, robots);
        return new_RobotTeam;
    }
    
    private IMailDelivery getDeliveryItem() {
		return this.delivery;
	}

	public IMailPool getMailPool() {
    	return this.mailPool;
    }
    
    public RobotLoader getRobotLoader() {
		return this.robotloader;
	}

	public int getDeliveryCounter() {
		return this.deliveryCounter;
	}

	public void setTeamSize() {
    	this.teamSize = robots.size();
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
            		// Makes sure that Team size has been updated in case of change
            		setTeamSize();
            		for(int i = 0; i < teamSize;i++) {
            			mailPool.addToPool(robots.get(i).getTube());
            			System.out.printf("T: %3d > old addToPool [%s]%n", Clock.Time(), robots.get(i).getTube().toString());
            			robots.get(i).setTube(null);
            		}
            	}
    			/** Tell the sorter the robot is ready */
            	// Makes sure that Team size has been updated in case of change
            	setTeamSize();
            	// Adds the robots in the team back into the pool
            	for(int i = 0; i < teamSize; i++) {
            		robotloader.registerWaiting(robots.get(i));
            	}
            	// Removes the team from Automail
            	Automail.robotTeam.remove(this);
            } else {
            	/** If the robot is not at the mailroom floor yet, then move towards it! */
                moveTowards(Building.MAILROOM_LOCATION);
            	break;
            }
        //Should Never be the case as if team is waiting it should be destroyed
		case WAITING:
            /** If the StorageTube is ready and the Robot is waiting in the mailroom then start the delivery */
            if(!isEmpty() && receivedDispatch){
            	receivedDispatch = false;
            	deliveryCounter = 0; // reset delivery counter
    			setRoute();
    			for(int i=0; i < teamSize;i++) {
    				changeState(RobotState.DELIVERING);
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
                
                // Create a new Team of size 1 to increase speed
                /** Check if other robots want to return, i.e. if there is no item in the tube*/
                for(int i=1; i < teamSize;i++) {
                	if(robots.get(i).getTube() == null){
                		ArrayList<Robot> new_ReturningTeam = new ArrayList<>();
                		new_ReturningTeam.add(robots.get(i));
                		// Remove Robot from current Team
                		robots.remove(i);
                		RobotTeam Ret_Team = createNewRobotTeam(this.getDeliveryItem(), 
                				this.getMailPool(),this.getRobotLoader(), new_ReturningTeam);
                		Ret_Team.changeState(RobotState.RETURNING);
                		Automail.robotTeam.add(Ret_Team);
                	}else {
                		ArrayList<Robot> new_robotTeam = new ArrayList<>();
                		new_robotTeam.add(robots.get(i));
                		// Remove Robot from current Team
                		robots.remove(i);
                		RobotTeam Retur_Team = createNewRobotTeam(this.getDeliveryItem(), 
                				this.getMailPool(),this.getRobotLoader(), new_robotTeam);
                		try {
							Retur_Team.addToHand(Retur_Team.robots.get(0).getTube());
							Retur_Team.robots.get(0).removeTube();
						} catch (ItemTooHeavyException e) {
						}
                		Automail.robotTeam.add(Retur_Team);
                	}
                }
                // Set team size after removal
                setTeamSize();
                setSpeed();
                // There should only be 1 robot in the team
                if(robots.get(0).getTube() == null){
                	this.changeState(RobotState.DELIVERING);
                }else {
                	this.changeState(RobotState.DELIVERING);
                }
                
			} else {
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
    		robots.get(i).set_current_floor(current_floor);
    	}
    }

    private String getIdTube() {
    	return String.format("%s(%1d)", id, (tube == null ? 0 : 1));
    }
    
    
    
    /**
     * Prints out the change in state
     * @param robot 
     * @param nextState the state to which the robot is transitioning
     */
    // May need to change
    private void changeState(RobotState nextState){
    	assert(!(deliveryItem == null && tube != null));
    	if (current_state != nextState) {
    		System.out.printf("T: %3d > %7s changed from %s to %s%n", Clock.Time(), getIdTube(), current_state, nextState);
    	}
    	current_state = nextState;
    	// Changes the state of all the robots for the whole team
    	for(int i = 0; i < teamSize; i++) {
    		robots.get(i).setState(nextState);
    	}
    	if(nextState == RobotState.DELIVERING){
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
		boolean carryingSomething = false;
		for(int i= 0; i< teamSize; i++) {
			if(robots.get(i) != null) {
				carryingSomething = true;
			}
		}
		if(deliveryItem != null) {
			carryingSomething = true;
		}
		return carryingSomething;
	}

	public void addToHand(MailItem mailItem)throws ItemTooHeavyException{
		assert(deliveryItem == null);
		deliveryItem = mailItem;
		if (deliveryItem.getWeight() > max_weight)throw new ItemTooHeavyException();
	}

	public void addToTube(MailItem mailItem) throws ItemTooHeavyException {
		for(int i = 0; i < teamSize; i++) {
			if(robots.get(i).getTube() == null) {
				robots.get(i).setTube(mailItem);
				if (tube.weight > max_weight) throw new ItemTooHeavyException();
				break;
			}
		}
	}
}
