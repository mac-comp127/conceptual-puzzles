package edu.macalester.conceptual.util;

/**
 * A wrapper to compare objects based on their object identity instead of their contents. This can
 * allow mutable objects to be used as keys in a Map, with the caveat that only the same object
 * can retrieve the value for a given key.
 */
public record IdentityKey<T>(T object) {
    @Override
    public boolean equals(Object o) {
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        IdentityKey<?> that = (IdentityKey<?>) o;
        return this.object == that.object;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(object);
    }
}
