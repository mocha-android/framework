/**
 *  @author Shaun
 *  @date 2/24/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.foundation;

import java.util.Map;

public class Notification extends MObject {

	private String name;
	private java.lang.Object sender;
	private Map<String,java.lang.Object> info;

	/**
	 * Create a new notification
	 *
	 * @param name Name of the notification (a RuntimeException will be thrown if null)
	 * @param sender Sender of the notification or null
	 */
	public Notification(String name, java.lang.Object sender) {
		this(name, sender, null);
	}

	/**
	 * Create a new notification
	 *
	 * @param name Name of the notification (a RuntimeException will be thrown if null)
	 * @param sender Sender of the notification or null
	 * @param info Additional information for the notification or null
	 */
	public Notification(String name, java.lang.Object sender, Map<String,java.lang.Object> info) {
		if(name == null) {
			throw new RuntimeException("Notification name can not be null.");
		}

		this.name = name;
		this.sender = sender;
		this.info = info;
	}

	/**
	 * Name of the notification
	 *
	 * @return Notification name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sender of the notification
	 *
	 * @return Sender of the notification (may be null)
	 */
	public java.lang.Object getSender() {
		return this.sender;
	}

	/**
	 * Additional information regarding the notifcation
	 *
	 * @return Notification information (may be null)
	 */
	public Map<String,java.lang.Object> getInfo() {
		return this.info;
	}

}
