/**
 *  @author Shaun
 *  @date 2/24/13
 *  @copyright 2013 enormego. All rights reserved.
 */
package mocha.foundation;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
		WeakReference<java.lang.Object> target;
		Method method;
		boolean methodTakesNotificationParameter;

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
			if(observation.observer != null) {
				Observer observer = observation.observer.get();

				if(observer != null) {
					observer.observe(notification);
				} else {
					MWarn("Trying to send notification %s to a GC'd observer.", name);
				}
			} else if(observation.target != null) {
				java.lang.Object target = observation.target.get();

				if(target != null) {
					try {
						if(observation.methodTakesNotificationParameter) {
							observation.method.invoke(target, notification);
						} else {
							observation.method.invoke(target);
						}
					} catch (IllegalAccessException e) {
						MWarn(e, "Could not post notification to %s#%s", target, observation.method);
					} catch (InvocationTargetException e) {
						throw new RuntimeException(e.getCause());
					}
				} else {
					MWarn("Trying to send notification %s to a GC'd observer.", name);
				}
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
	 * Add a target/action observer for a notification name and/or sender.
	 *
	 * @see NotificationCenter#addObserver(mocha.foundation.NotificationCenter.Observer, String, java.lang.Object)
	 *
	 * @param target Notification observer target to send action to
	 * @param action Action to send to target, should accept a single Notification parameter or none
	 * @param notificationName Notification name to observe or null
	 * @param notificationSender Notification sender to observer or null
	 */
	@SuppressWarnings("unchecked")
	public void addObserver(java.lang.Object target, Method action, String notificationName, java.lang.Object notificationSender) {
		if(notificationName == null && notificationSender == null) {
			throw new RuntimeException("You must observe at least a notification name or a notification sender.");
		}

		if(target == null || action == null) {
			throw new RuntimeException("You must provide both a target and an action.");
		}

		boolean passedParameterCheck;
		boolean methodTakesNotificationParameter = action.getParameterTypes().length == 1;

		if(methodTakesNotificationParameter) {
			Class parameter = action.getParameterTypes()[0];
			passedParameterCheck = !(parameter != Notification.class && !parameter.isAssignableFrom(Notification.class));
		} else {
			passedParameterCheck = action.getParameterTypes().length == 0;
		}

		if(!passedParameterCheck) {
			throw new RuntimeException("Notification target action can only accept a single Notification parameter or no parameters at all.");
		}

		MLog("Adding target/action observer %s %s for %s", target, action, notificationName);

		Observeration observation = new Observeration();
		observation.target = new WeakReference<java.lang.Object>(target);
		observation.method = action;
		observation.methodTakesNotificationParameter = methodTakesNotificationParameter;
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
	 * Add a target/action observer for a notification name and/or sender.
	 *
	 * @see NotificationCenter#addObserver(java.lang.Object, java.lang.reflect.Method, String, java.lang.Object)
	 *
	 * @param target Notification observer target to send action to
	 * @param actionMethodName Name of the action method to send to target, should accept a single Notification parameter or none.
	 * @param notificationName Notification name to observe or null
	 * @param notificationSender Notification sender to observer or null
	 */
	public void addObserver(java.lang.Object target, String actionMethodName, String notificationName, java.lang.Object notificationSender) {
		Method method;

		try {
			method = target.getClass().getMethod(actionMethodName, Notification.class);
		} catch (NoSuchMethodException e) {
			try {
				method = target.getClass().getMethod(actionMethodName);
			} catch (NoSuchMethodException e1) {
				throw new RuntimeException("Could not find method " + actionMethodName + " on target " + target + " that accepts a Notification parameter or none at all.");
			}
		}

		this.addObserver(target, method, notificationName, notificationSender);
	}

	/**
	 * Remove an observer from all previously added observations
	 * regardless of their notificaiton name or notificaiton sender.
	 *
	 * @param observer Observer to remove
	 */
	public void removeObserver(java.lang.Object observer) {
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
	public void removeObserver(java.lang.Object observer, String notificationName, java.lang.Object notificationSender) {
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

	private void removeObserver(List<Observeration> observations, java.lang.Object observer, String name, java.lang.Object sender) {
		Iterator<Observeration> iterator = observations.iterator();

		while (iterator.hasNext()) {
			Observeration observation = iterator.next();
			Observer observer1 = observation.observer == null ? null : observation.observer.get();
			java.lang.Object target = observation.target == null ? null : observation.target.get();
			if((observer1 == null && target == null) || ((observer1 == observer || target == observation) && observation.isObserving(name, sender))) {
				iterator.remove();
			}
		}
	}

}
