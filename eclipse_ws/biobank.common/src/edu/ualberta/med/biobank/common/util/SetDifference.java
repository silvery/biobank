package edu.ualberta.med.biobank.common.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Will not work if oldSet or newSet are modified after creating this object.
 * 
 * Symetric difference can be obtained by performing a union on removeSet and
 * addSet.
 * 
 * @param <T>
 */
public class SetDifference<T> {

    private Set<T> oldSet;
    private Set<T> newSet;
    private Set<Pair<T>> intersection;
    private Set<T> removeSet;
    private Set<T> addSet;

    public static class Pair<T> {
        public T oldObject;
        public T newObject;

        public Pair(T oldObj, T newObj) {
            this.oldObject = oldObj;
            this.newObject = newObj;
        }
    }

    public SetDifference(Set<T> oldSet, Set<T> newSet) {
        this.oldSet = oldSet;
        this.newSet = newSet;

        intersection = new HashSet<Pair<T>>();

        Set<T> oldObjIntersection = new HashSet<T>(oldSet);
        oldObjIntersection.retainAll(newSet);

        Set<T> newObjIntersection = new HashSet<T>(newSet);
        newObjIntersection.retainAll(oldSet);

        for (T oldObj : oldObjIntersection) {
            for (T newOjb : newObjIntersection) {
                if (oldObj.equals(newOjb)) {
                    intersection.add(new Pair<T>(oldObj, newOjb));
                }
            }
        }

        removeSet = new HashSet<T>(oldSet);
        removeSet.removeAll(oldObjIntersection);

        addSet = new HashSet<T>(newSet);
        addSet.removeAll(oldObjIntersection);
    }

    /**
     * May be called with one of the parameters being the getXxxxCollection()
     * method on a model object, so check for null.
     */
    public SetDifference(Collection<T> oldSet, Collection<T> newSet) {
        this(oldSet != null ? new HashSet<T>(oldSet) : new HashSet<T>(),
            newSet != null ? new HashSet<T>(newSet) : new HashSet<T>());
    }

    public Set<T> getOldSet() {
        return oldSet;
    }

    public Set<T> getNewSet() {
        return newSet;
    }

    public Set<Pair<T>> getIntersection() {
        return intersection;
    }

    public Set<T> getRemoveSet() {
        return removeSet;
    }

    public Set<T> getAddSet() {
        return addSet;
    }
}
