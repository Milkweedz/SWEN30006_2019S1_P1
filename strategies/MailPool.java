package strategies;

import java.util.LinkedList;
import java.util.Comparator;
import automail.Item;
import automail.MailItem;
public class MailPool implements IMailPool {
	
	public class ItemComparator implements Comparator<Item> {
		@Override
		public int compare(Item i1, Item i2) {
			int order = 0;
			if (i1.getPriority() < i2.getPriority()) {
				order = 1;
			} else if (i1.getPriority() > i2.getPriority()) {
				order = -1;
			} else if (i1.getDestination() < i2.getDestination()) {
				order = 1;
			} else if (i1.getDestination() > i2.getDestination()) {
				order = -1;
			}
			return order;
		}
	}
	
	private LinkedList<Item> pool;
//	private LinkedList<RobotTeam> teams;

	public MailPool(int nmail){
		// Start empty
		pool = new LinkedList<Item>();
	}

	public void addToPool(MailItem mailItem) {
		Item item = new Item(mailItem);
		pool.add(item);
		pool.sort(new ItemComparator());
	}
	
	public LinkedList<Item> get_pool(){
		return pool;
	}

}
