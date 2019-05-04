package automail;

import exceptions.ExcessiveDeliveryException;
import exceptions.ItemTooHeavyException;
import strategies.IMailPool;
import strategies.MailPool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class RobotLoader {

    static public final int INDIVIDUAL_MAX_WEIGHT = 2000;
    static public final int PAIR_MAX_WEIGHT = 2600;
    static public final int TRIPLE_MAX_WEIGHT = 3000;

    private IMailPool mailPool;
    private IMailDelivery delivery;

    private static LinkedList<Robot> robots;   // free robots without team
    private LinkedList<RobotTeam> teams;
    private LinkedList<RobotTeam> finishedTeams;    // teams that finish their hand item delivery
    private LinkedList<RobotTeam> idleTeams;        // teams in mailroom waiting for mail
    public static ArrayList<RobotTeam> robotTeamList = new ArrayList<>();

    private static RobotLoader instance = null;

    public static RobotLoader getInstance(IMailDelivery delivery, IMailPool mailPool, Robot[] robots){
        if (instance == null) {
            instance = new RobotLoader(delivery, mailPool, robots);
        }
        return instance;
    }

    public RobotLoader(IMailDelivery delivery, IMailPool mailPool, Robot[] robots){

        this.setMailPool(mailPool);
        this.delivery = delivery;
        this.robots = new LinkedList<Robot>();
        this.teams = new LinkedList<RobotTeam>();
        this.finishedTeams = new LinkedList<RobotTeam>();
        this.idleTeams = new LinkedList<RobotTeam>();

        for (Robot robot : robots){
            this.robots.add(robot);
        }
    }

    private void setMailPool(IMailPool mailPool2) {
		this.mailPool = mailPool2;
	}

	public void loadRobotTeam(ListIterator<Robot> r) throws ItemTooHeavyException {
        try{
            ListIterator<MailPool.Item> m = getMailPool().getPool();
            if (m.hasNext()) {
                MailItem mail = m.next().mailItem;

                if (enoughRobots(mail.weight, r)){
                	listReset(r);
                	ArrayList<Robot> teamMembers = new ArrayList<>();
                	for (int i=0; i<numBotsNeeded(mail.weight); i++){
                    	Robot robotmem = r.next();
                		teamMembers.add(robotmem);
                		r.remove();
                		listReset(r);
                	}
                	
                    RobotTeam team = new RobotTeam(mailPool, delivery, this, teamMembers);
                    System.out.println("New Robot Team Created! id: " + team.id);
                    System.out.println("TeamSize: " + team.robots.size());
                    robotTeamList.add(team);
                    assert(team.isEmpty());
                    team.addToHand(mail); // hand first as we want higher priority delivered first
                    team.setSpeed();
                    m.remove();
                    
                    for (Robot robot : team.robots){
                        if (!m.hasNext()) break;
                        mail = m.next().mailItem;
                        
                        if (mail.weight <= INDIVIDUAL_MAX_WEIGHT) {
                            robot.addToTube(mail);
                            m.remove();
                        }
                    }

                    teams.add(team);
                    
                    team.dispatch(); // send the team off if it has any items to deliver
                    //r.remove();       // remove from mailPool queue
                }
            }else {
            	System.out.println("Not Enough Robots or Mail");
                r.next();
            }
        } catch (Exception e) {
            throw e;
        }
    }

    private IMailPool getMailPool() {
		return this.mailPool;
	}

	public void step() throws ExcessiveDeliveryException, ItemTooHeavyException {
        try {
            ListIterator<RobotTeam> ft = this.finishedTeams.listIterator();
            while (ft.hasNext()){
                RobotTeam oldTeam = ft.next();
                ft.remove();
                for (Robot robot : oldTeam.robots){
                    ArrayList<Robot> teamMembers = new ArrayList<>();
                    teamMembers.add(robot);
                    RobotTeam newTeam = new RobotTeam(mailPool,delivery, this, teamMembers);
                    teams.push(newTeam);
                    newTeam.tubeToHand();
                }
            }

            ListIterator<Robot> r = this.robots.listIterator();
            while (r.hasNext()) {
                loadRobotTeam(r);
            }

            ListIterator<RobotTeam> t = this.teams.listIterator();
            if (t.hasNext()){
                t.next().step();
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public void registerWaiting(RobotTeam team) { // at mailroom
        this.robots.add(team.robots.get(0));
        team = null;
    }
    
    
    
    public void registerFinished(RobotTeam team) { // assumes won't be there already
        teams.remove(team);
        finishedTeams.add(team);
    }

//    public void splitTeam(RobotTeam oldTeam){
//        for (Robot robot : oldTeam.robots){
//            Robot[] teamMembers = new Robot[1];
//            teamMembers[0] = robot;
//            RobotTeam newTeam = new RobotTeam(delivery, this, teamMembers);
//            teams.push(newTeam);
//            newTeam.dispatch();
//        }
//    }


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
    
    public void listReset(ListIterator<Robot> r) {
    	while(r.hasPrevious()) {
    		r.previous();
    	}
    }
    
    public int listSize(ListIterator<Robot> r) {
    	int i = 0;
    	while(r.hasNext()) {
    		i++;
    		r.next();
    	}
    	return i; 
    }

    private boolean enoughRobots(int weight,ListIterator<Robot> r) throws ItemTooHeavyException{
    	listReset(r);
    	int i = 0;
    	while(r.hasNext()) {
    		i++;
    		r.next();
    	}
        int numbots = i;
        System.out.println(numbots + " are currently in the pool");
        int need = numBotsNeeded(weight);
        System.out.println(need + " are needed");
        if (need == -1) {
            throw new ItemTooHeavyException();
        }

        // return true or false - do we have enough bots
        return (numbots >= numBotsNeeded(weight));
    }
}
