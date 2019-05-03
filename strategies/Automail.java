package strategies;

import java.util.ArrayList;

import automail.IMailDelivery;
import automail.Robot;
import automail.RobotTeam;

public class Automail {
	      
    public Robot[] robots;
    public IMailPool mailPool;
    public RobotLoader robotloader;
    public static ArrayList<RobotTeam> robotTeam = new ArrayList<>();
    
    public Automail(IMailPool mailPool, IMailDelivery delivery, int numRobots, RobotLoader robotloader) {
    	// Swap between simple provided strategies and your strategies here
    	    	
    	/** Initialize the MailPool */
    	
    	this.mailPool = mailPool;
    	
    	/** Initialize the RobotLoader */
    	this.robotloader = robotloader;
    	
    	/** Initialize robots */
    	
    	robots = new Robot[numRobots];
    	for (int i = 0; i < numRobots; i++) robots[i] = new Robot();
    }
    
    public void removeRobotTeam(RobotTeam robotteam) {
    	this.removeRobotTeam(robotteam);
    }
    public ArrayList<RobotTeam> getRobotTeam() {
    	return(this.robotTeam);
    }
}
