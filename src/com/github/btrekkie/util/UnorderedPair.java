package com.github.btrekkie.util;

/** An immutable, unordered pair of values of type T. */
public class UnorderedPair<T> {
    /** The first value. */
    public final T value1;

    /** The second value. */
    public final T value2;

    public UnorderedPair(T value1, T value2) {
        this.value1 = value1;
        this.value2 = value2;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UnorderedPair)) {
            return false;
        }
        UnorderedPair<?> pair = (UnorderedPair<?>)obj;
        if (value1 != null && value2 != null) {
            return (value1.equals(pair.value1) && value2.equals(pair.value2)) ||
                (value1.equals(pair.value2) && value2.equals(pair.value1));
        } else if (value1 == null && value2 == null) {
            return pair.value1 == null && pair.value2 == null;
        } else if (value1 == null) {
            if (pair.value1 == null) {
                return value2.equals(pair.value2);
            } else {
                return pair.value2 == null && value2.equals(pair.value1);
            }
        } else if (pair.value1 == null) {
            return value1.equals(pair.value2);
        } else {
            return pair.value2 == null && value1.equals(pair.value1);
        }
    }

    @Override
    public int hashCode() {
        int hash1;
        if (value1 != null) {
            hash1 = value1.hashCode();
        } else {
            hash1 = 404815078;
        }
        int hash2;
        if (value2 != null) {
            hash2 = value2.hashCode();
        } else {
            hash2 = 404815078;
        }
        return hash1 + hash2;
    }

    @Override
    public String toString() {
        return "[UnorderedPair value1=" + value1 + ", value2=" + value2 + "]";
    }
}
