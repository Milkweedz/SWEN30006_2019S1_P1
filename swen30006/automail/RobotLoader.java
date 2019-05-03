package automail;

import exceptions.ExcessiveDeliveryException;
import exceptions.ItemTooHeavyException;
import strategies.IMailPool;
import strategies.MailPool;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

public class RobotLoader {

    static public final int INDIVIDUAL_MAX_WEIGHT = 2000;
    static public final int PAIR_MAX_WEIGHT = 2600;
    static public final int TRIPLE_MAX_WEIGHT = 3000;

    private IMailPool mailPool;
    private IMailDelivery delivery;

    private LinkedList<Robot> robots;   // free robots without team
    private LinkedList<RobotTeam> teams;
    private LinkedList<RobotTeam> finishedTeams;    // teams that finish their hand item delivery
    private LinkedList<RobotTeam> idleTeams;        // teams in mailroom waiting for mail

    private static RobotLoader instance = null;

    public static RobotLoader getInstance(IMailDelivery delivery, IMailPool mailPool, Robot[] robots){
        if (instance == null) {
            instance = new RobotLoader(delivery, mailPool, robots);
        }
        return instance;
    }

    public RobotLoader(IMailDelivery delivery, IMailPool mailPool, Robot[] robots){

        this.mailPool = mailPool;
        this.delivery = delivery;
        this.robots = new LinkedList<Robot>();
        this.teams = new LinkedList<RobotTeam>();
        this.finishedTeams = new LinkedList<RobotTeam>();
        this.idleTeams = new LinkedList<RobotTeam>();

        for (Robot robot : robots){
            this.robots.add(robot);
        }
    }

    public void loadRobotTeam(ListIterator<Robot> r) throws ItemTooHeavyException {
        try{
            ListIterator<MailPool.Item> m = mailPool.getPool();
            System.out.println("DEBUG" + Clock.Time());
            if (m.hasNext()) {
                MailItem mail = m.next().mailItem;

                if (enoughRobots(mail.weight)){
                    Robot[] teamMembers = new Robot[numBotsNeeded(mail.weight)];
                    for (int i=0; i<teamMembers.length; i++){
                        teamMembers[i] = r.next();
                        r.remove();
                    }
                    RobotTeam team = new RobotTeam(delivery, this, teamMembers);
                    assert(team.isEmpty());
                    team.addToHand(mail); // hand first as we want higher priority delivered first
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
            }
            else {
                r.next();
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public void step() throws ExcessiveDeliveryException, ItemTooHeavyException {
        try {
            ListIterator<RobotTeam> ft = this.finishedTeams.listIterator();
            while (ft.hasNext()){
                RobotTeam oldTeam = ft.next();
                ft.remove();
                for (Robot robot : oldTeam.robots){
                    Robot[] teamMembers = new Robot[1];
                    teamMembers[0] = robot;
                    RobotTeam newTeam = new RobotTeam(delivery, this, teamMembers);
                    teams.push(newTeam);
                    newTeam.tubeToHand();
//                    newTeam.dispatch();
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
        teams.remove(team);

        for (Robot robot : team.robots){
            robots.add(robot);
        }
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
