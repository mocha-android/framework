/**
 *  @author Shaun
 *  @date 2/24/13
 *  @copyright 2013 enormego. All rights reserved.
 */
package mocha.foundation;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.Semaphore;

public class NotificationCenter extends Object {

	public interface Observer {
		/**
		 * Called by notification center when a notification is posted that
		 * matches the criteria for what we're observing.
		 *
		 * This will be called on the thread that posted the notification,
		 * not the thread that the observer was added on.
		 *
		 * @param notification notification posted
		 */
		public void observe(Notification notification);
	}

	private static class Observeration {
		WeakReference<Observer> observer;
		WeakReference<java.lang.Object> sender;
		String name;

		boolean isObserving(String name, java.lang.Object sender) {
			boolean nameMatch = name == null || (this.name != null && this.name.equals(name));
			boolean senderMatch = sender == null || (this.sender.get() == sender);
			return nameMatch && senderMatch;
		}
	}

	private static NotificationCenter defaultCenter = new NotificationCenter();
	private Map<String,List<Observeration>> observationsByName = new HashMap<String, List<Observeration>>();
	private Map<Integer,List<Observeration>> observationsBySender = new HashMap<Integer, List<Observeration>>();

	private Semaphore lock = new Semaphore(1);

	/**
	 * Get the default notification center
	 *
	 * @return notification center
	 */
	public static NotificationCenter defaultCenter() {
		return defaultCenter;
	}

	/**
	 * Post a notification with it's name and sender.
	 *
	 * Posts notification on the current thread and waits to return until
	 * all observers have been notified.
	 *
	 * @param notificationName Name of the notification (a RuntimeException will be thrown if null)
	 * @param notificationSender Sender of the notification or null
	 */
	public void post(String notificationName, java.lang.Object notificationSender) {
		this.post(notificationName, notificationSender, null);
	}

	/**
	 * Post a notification with it's name and sender, as well as additional info.
	 *
	 * Posts notification on the current thread and waits to return until
	 * all observers have been notified.
	 *
	 * @param notificationName Name of the notification (a RuntimeException will be thrown if null)
	 * @param notificationSender Sender of the notification or null
	 * @param info Additional information to be included with the notification or null
	 */
	public void post(String notificationName, java.lang.Object notificationSender, Map<String,java.lang.Object> info) {
		this.post(new Notification(notificationName, notificationSender, info));
	}

	/**
	 * Post a notification
	 *
	 * Posts notification on the current thread and waits to return until
	 * all observers have been notified.
	 *
	 * @param notification Notification to post (a RuntimeException will be thrown if null)
	 */
	public void post(Notification notification) {
		if(notification == null) {
			throw new RuntimeException("You can not post a null notification");
		}

		String name = notification.getName();
		java.lang.Object sender = notification.getSender();

		Set<Observeration> observations = new HashSet<Observeration>();

		this.lock.acquireUninterruptibly();

		List<Observeration> observationsByName = this.observationsByName.get(name);
		if(observationsByName != null) {
			for(Observeration observation : observationsByName) {
				if(observation.isObserving(name, sender)) {
					observations.add(observation);
				}
			}
		}

		for(List<Observeration> observationsBySender : this.observationsBySender.values()) {
			for(Observeration observation : observationsBySender) {
				if(observation.isObserving(name, sender)) {
					observations.add(observation);
				}
			}
		}

		this.lock.release();

		for(Observeration observation : observations) {
			Observer observer = observation.observer.get();

			if(observer != null) {
				observer.observe(notification);
			} else {
				MWarn("Trying to send notification %s to a GC'd observer.", name);
			}
		}
	}

	/**
	 * Add an observer for a notification name and/or sender.
	 *
	 * An observer must observe at least a notification name or sender. If both
	 * notificationName and notificationSender are null, a RuntimeException will
	 * be thrown.
	 * <ul>
	 * <li>If notificationName is null, the observer will receive all notifications
	 * posted by notificationSender</li>
	 * <li>If notificationSender is null, the observer will receive all notifications
	 * posted with notificationName as it's name.</li>
	 * </ul>
	 * <strong>Important:</strong><br />
	 * You can not add an anonymous class directly as an observer.  Observers are stored
	 * with weak references to ensure NotificationCenter doesn't prevent it from being
	 * GC'd.  Directly adding an anonymous class as an observer will cause it to be GC'd
	 * on the GC next run. To work around this, you can create the observer as an anonymous
	 * class and set to to an instance variable on your class:
	 * <pre>
	 * {@code
	 * this.myObserver = new Observer() {
	 * 	public void observe(Notification notification) {
	 *
	 * 	}
	 * };
	 *
	 * NotificationCenter.defaultCenter().addObserver(this.myObserver, "name", null);
	 * }
	 * </pre>
	 *
	 * @param observer Notification observer
	 * @param notificationName Notification name to observe or null
	 * @param notificationSender Notification sender to observer or null
	 */
	public void addObserver(Observer observer, String notificationName, java.lang.Object notificationSender) {
		if(notificationName == null && notificationSender == null) {
			throw new RuntimeException("You must observe at least a notification name or a notification sender.");
		}

		Observeration observation = new Observeration();
		observation.observer = new WeakReference<Observer>(observer);
		observation.name = notificationName;

		if(notificationSender != null) {
			observation.sender = new WeakReference<java.lang.Object>(notificationSender);
		}

		this.lock.acquireUninterruptibly();

		if(notificationName != null) {
			List<Observeration> observations = this.observationsByName.get(notificationName);

			if(observations == null) {
				observations = new ArrayList<Observeration>();
				this.observationsByName.put(notificationName, observations);
			}

			observations.add(observation);
		}

		if(notificationSender != null) {
			Integer sender = notificationSender.hashCode();
			List<Observeration> observations = this.observationsBySender.get(sender);

			if(observations == null) {
				observations = new ArrayList<Observeration>();
				this.observationsBySender.put(sender, observations);
			}

			observations.add(observation);
		}

		this.lock.release();
	}

	/**
	 * Remove an observer from all previously added observations
	 * regardless of their notificaiton name or notificaiton sender.
	 *
	 * @param observer Observer to remove
	 */
	public void removeObserver(Observer observer) {
		this.removeObserver(observer, null, null);
	}

	/**
	 * Remove all matching observations for the observer based on the provided
	 * notificaitonName and notificationSender values.
	 *
	 * @param observer Observer to remove
	 * @param notificationName Name of the notification to remove for this observer. If null,
	 *                         the notificaton name will not be used when determining whether or
	 *                         not the observation matches.
	 * @param notificationSender Notification sender to remove for this observer. If null,
	 *                              the notificaton sender will not be used when determining whether or
	 *                              not the observation matches.
	 */
	public void removeObserver(Observer observer, String notificationName, java.lang.Object notificationSender) {
		if(observer == null) return;

		this.lock.acquireUninterruptibly();

		if(notificationName != null) {
			List<Observeration> observations = this.observationsByName.get(notificationName);

			if(observations != null) {
				this.removeObserver(observations, observer, notificationName, notificationSender);

				if(observations.size() == 0) {
					this.observationsByName.remove(notificationName);
				}
			}
		} else {
			Iterator<Map.Entry<String,List<Observeration>>> iterator = this.observationsByName.entrySet().iterator();

			while (iterator.hasNext()) {
				Map.Entry<String,List<Observeration>> entry = iterator.next();
				List<Observeration> observations = entry.getValue();
				this.removeObserver(observations, observer, null, notificationSender);

				if(observations.size() == 0) {
					iterator.remove();
				}
			}
		}

		if(notificationSender != null) {
			Integer sender = notificationSender.hashCode();
			List<Observeration> observations = this.observationsBySender.get(sender);

			if(observations != null) {
				this.removeObserver(observations, observer, notificationName, notificationSender);

				if(observations.size() == 0) {
					this.observationsBySender.remove(sender);
				}
			}
		} else {
			Iterator<Map.Entry<Integer,List<Observeration>>> iterator = this.observationsBySender.entrySet().iterator();

			while (iterator.hasNext()) {
				Map.Entry<Integer,List<Observeration>> entry = iterator.next();
				List<Observeration> observations = entry.getValue();
				this.removeObserver(observations, observer, notificationName, null);

				if(observations.size() == 0) {
					iterator.remove();
				}
			}
		}

		this.lock.release();
	}

	private void removeObserver(List<Observeration> observations, Observer observer, String name, java.lang.Object sender) {
		Iterator<Observeration> iterator = observations.iterator();

		while (iterator.hasNext()) {
			Observeration observation = iterator.next();
			Observer observer1 = observation.observer.get();
			if(observer1 == null || (observer1 == observer && observation.isObserving(name, sender))) {
				MLog("Trying to remove %s", observer1);
				iterator.remove();
			} else {
				MLog("%s isn't observing %s %s", name, sender);
			}
		}
	}

}
