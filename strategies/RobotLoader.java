package strategies;

import exceptions.ItemTooHeavyException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import automail.IMailDelivery;
import automail.Item;
import automail.MailItem;
import automail.Robot;
import automail.Robot.RobotState;
import automail.RobotTeam;

public class RobotLoader {

    static public final int INDIVIDUAL_MAX_WEIGHT = 2000;
    static public final int PAIR_MAX_WEIGHT = 2600;
    static public final int TRIPLE_MAX_WEIGHT = 3000;

    private MailPool mailPool;
    private IMailDelivery delivery;

    private LinkedList<Robot> robots;
  
    public RobotLoader(int nRobots) {
    	robots = new LinkedList<Robot>();
    }
    
    public void step() throws ItemTooHeavyException {
    	// Get list of available robots
    	LinkedList<Robot> i = robots;
    	// Get the first robot on the list
    	Robot robot = i.getFirst();
        // Checks if individual Robot has nothing in it's tube
        assert(robot.isEmpty());
        // Gets the mailpool
        ListIterator<Item> j = mailPool.get_pool().listIterator();
        // Gets the fist Priority mail Item
        MailItem mailItem = j.next().getMailItem();
        // try to make a team capable of carrying the mail item
        try {
        	if (enoughRobots(mailItem.getWeight())){
        		ArrayList<Robot> teamMembers = new ArrayList<>(); 
        		for (int k=0; k<numBotsNeeded(mailItem.getWeight()); k++){
        			teamMembers.add(robot);
        			// Remove the first from the waiting robot list
        			robots.removeFirst();
        			// Make the new robot the first robot on the list
        			robot = robots.getFirst();
        		}
        		RobotTeam robotTeam = new RobotTeam(delivery, mailPool, this,teamMembers);
        		loadRobotTeam(robotTeam, j);
        		Automail.robotTeam.add(robotTeam);
        	}
        // If there is not enough robots in the pool exception handle
        }catch(Exception e){
        	throw e;
        }
    }

    private void loadRobotTeam(RobotTeam team, ListIterator<Item> j) throws ItemTooHeavyException {        
        if (mailPool.get_pool().size() > 0) {
        	team.addToHand(j.next().getMailItem()); // hand first as we want higher priority delivered first
            j.remove();	// Remove mail from the pool
            if (mailPool.get_pool().size() > 0) {
            	team.addToTube(j.next().getMailItem());
            	j.remove();
            }
            team.dispatch(); // send the team off if it has any items to deliver
        }
    }

    public void registerWaiting(Robot robot) {
    	//sets the robot state to waiting
    	robot.setState(RobotState.WAITING);
        this.robots.add(robot);
    }


    private int numBotsNeeded(int weight) {
        if (weight > TRIPLE_MAX_WEIGHT) {
            return -1;
        }
        else if (weight > PAIR_MAX_WEIGHT) {
            return 3;
        }
        else if (weight > INDIVIDUAL_MAX_WEIGHT) {
            return 2;
        }
        else { return 1; }
    }

    private boolean enoughRobots(int weight) throws ItemTooHeavyException{
        int numbots = this.robots.size();
        int need = numBotsNeeded(weight);

        if (need == -1) {
            throw new ItemTooHeavyException();
        }

        // return true or false - do we have enough bots
        return (numbots >= numBotsNeeded(weight));
    }
}
