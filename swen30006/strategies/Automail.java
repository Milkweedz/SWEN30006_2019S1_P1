package strategies;

import automail.IMailDelivery;
import automail.Robot;
import automail.RobotLoader;

public class Automail {
	      
    public Robot[] robots;
    public IMailPool mailPool;

    public RobotLoader robotLoader;
    
    public Automail(IMailPool mailPool, IMailDelivery delivery, int numRobots) {
    	// Swap between simple provided strategies and your strategies here
    	    	
    	/** Initialize the MailPool */
    	
    	this.mailPool = mailPool;
    	
    	/** Initialize robots */
    	robots = new Robot[numRobots];
    	for (int i = 0; i < numRobots; i++) robots[i] = new Robot();

    	/** Initilize robotloader */
    	this.robotLoader = RobotLoader.getInstance(delivery, mailPool, robots);
    }
    
}
