package automail;

import exceptions.ItemTooHeavyException;
import strategies.IMailPool;
import strategies.MailPool;

import java.util.LinkedList;
import java.util.ListIterator;

public class RobotLoader {

    static public final int INDIVIDUAL_MAX_WEIGHT = 2000;
    static public final int PAIR_MAX_WEIGHT = 2600;
    static public final int TRIPLE_MAX_WEIGHT = 3000;

    private IMailPool mailPool;
    private IMailDelivery delivery;

    private LinkedList<Robot> robots;
    private LinkedList<RobotTeam> teams;

    public void step(MailItem mailItem) throws ItemTooHeavyException {
        try{
            if (enoughRobots(mailItem.weight)){
                Robot[] teamMembers = new Robot[numBotsNeeded(mailItem.weight)];
                for (int i=0; i<teamMembers.length; i++){
                    teamMembers[i] = robots.remove();
                }
                RobotTeam robotTeam = new RobotTeam(delivery, mailPool, teamMembers);
            }
        } catch (Exception e) {
            throw e;
        }
    }

    private void loadRobotTeam(RobotTeam team) throws ItemTooHeavyException {
        assert(team.isEmpty());
        // System.out.printf("P: %3d%n", pool.size());
        ListIterator<MailPool.Item> j = pool.listIterator();
        if (pool.size() > 0) {
            try {
                team.addToHand(j.next().mailItem); // hand first as we want higher priority delivered first
                j.remove();
                if (pool.size() > 0) {
                    team.addToTube(j.next().mailItem);
                    j.remove();
                }
                team.dispatch(); // send the team off if it has any items to deliver
                i.remove();       // remove from mailPool queue
            } catch (Exception e) {
                throw e;
            }
        }
    }

    public void registerWaiting(RobotTeam team) { // assumes won't be there already
        teams.add(team);
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
