package com.jim.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** General purpose observer functionality. */
public class ObserverMgr {
    private final Map<Object, List<Observer>> observers = new ConcurrentHashMap<>();

    private static final ObserverMgr instance = new ObserverMgr();

    /** Interface to be implemented by Observers. */
    public interface Observer {
        /** Called when an observed object has changed.
         *
         * @param object The observed object which changed.
         */
        void notify(Object object);
    }

    /** Returns the single instance of this class. */
    public static ObserverMgr getInstance() {
        return instance;
    }

    /** Registers an Observer to watch for changes on an object. */
    public void observe(Object object, Observer observer) {
        List<Observer> list;
        if (!observers.containsKey(object)) {
            list = new CopyOnWriteArrayList<>();
            observers.put(object, list);
        } else {
            list = observers.get(object);
        }
        list.add(observer);
    }

    /** Fires change notification on the specified object.
     * All Observers registered for the object will be notified. */
    public void fire(Object object) {
        List<Observer> list = observers.get(object);
        if (list != null) {
            for (Observer o : list) {
                try {
                    o.notify(object);
                } catch (Exception e) {
                    System.err.println("Exception in Observer" + e);
                    e.printStackTrace(System.err);
                }
            }
        }
    }
}
