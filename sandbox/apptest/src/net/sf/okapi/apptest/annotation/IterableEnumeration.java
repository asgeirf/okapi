package net.sf.okapi.apptest.annotation;

import java.util.Enumeration;
import java.util.Iterator;

/**
 * 
 * Wrap an Enumeration and make it compatible with new Iterators. Useful for
 * dealing with old style API's.
 * 
 * @param <T>
 */
class IterableEnumeration<T> implements Iterable<T> {
	private final Enumeration<T> en;

	public IterableEnumeration(Enumeration<T> en) {
		this.en = en;
	}

	// return an adaptor for the Enumeration
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			public boolean hasNext() {
				return en.hasMoreElements();
			}

			public T next() {
				return en.nextElement();
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public static <T> Iterable<T> make(Enumeration<T> en) {
		return new IterableEnumeration<T>(en);
	}
}
