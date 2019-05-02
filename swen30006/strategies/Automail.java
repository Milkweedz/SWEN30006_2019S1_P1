package strategies;

import automail.IMailDelivery;
import automail.RobotTeam;

public class Automail {
	      
    public RobotTeam[] teams;
    public IMailPool mailPool;
    
    public Automail(IMailPool mailPool, IMailDelivery delivery, int numRobotTeams) {
    	// Swap between simple provided strategies and your strategies here
    	    	
    	/** Initialize the MailPool */
    	
    	this.mailPool = mailPool;
    	
    	/** Initialize teams */
    	teams = new RobotTeam[numRobotTeams];
    	for (int i = 0; i < numRobotTeams; i++) teams[i] = new RobotTeam(delivery, mailPool);
    }
    
}
