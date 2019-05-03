package automail;

public class Item {
	private int priority;
	private int destination;
	private MailItem mailItem;
	// Use stable sort to keep arrival time relative positions
	
	public Item(MailItem mailItem) {
		setPriority((mailItem instanceof PriorityMailItem) ? ((PriorityMailItem) mailItem).getPriorityLevel() : 1);
		setDestination(mailItem.getDestFloor());
		this.setMailItem(mailItem);
	}

	public MailItem getMailItem() {
		return mailItem;
	}

	public void setMailItem(MailItem mailItem) {
		this.mailItem = mailItem;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public int getDestination() {
		return destination;
	}

	public void setDestination(int destination) {
		this.destination = destination;
	}
}
