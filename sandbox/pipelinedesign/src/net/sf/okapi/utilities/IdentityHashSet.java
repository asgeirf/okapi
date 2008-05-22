/*
 * Copyright 2007  T-Rank AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.okapi.utilities;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * This class implements the <tt>Set</tt> interface with a hash table, using
 * reference-equality in place of object-equality when comparing objects. <p/>
 * In other words, in an <tt>IdentityHashSet</tt>, two objects <tt>k1</tt>
 * and <tt>k2</tt> are considered equal if and only if <tt>(k1==k2)</tt>.
 * (In normal <tt>Set</tt> implementations (like <tt>HashSet</tt>) two
 * objects <tt>k1</tt> and <tt>k2</tt> are considered equal if and only if
 * <tt>(k1==null ? k2==null : k1.equals(k2))</tt>.) <p/>
 * <p>
 * <b>This class is <i>not</i> a general-purpose <tt>Set</tt> implementation!
 * While this class implements the <tt>Set</tt> interface, it intentionally
 * violates <tt>Set's</tt> general contract, which mandates the use of the
 * <tt>equals</tt> method when comparing objects. This class is designed for
 * use only in the rare cases wherein reference-equality semantics are required.</b>
 *
 * @version $Revision: 112 $
 */
public class IdentityHashSet<E> extends AbstractSet<E> implements Set<E>,
		Cloneable, java.io.Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -4092340883273538226L;

	private transient IdentityHashMap<E, Object> map;

	// Dummy value to associate with an Object in the backing Map
	private static final Object PRESENT = new Object();

	/**
	 * Constructs a new, empty set; the backing <tt>HashMap</tt> instance has
	 * default initial capacity (16) and load factor (0.75).
	 */
	public IdentityHashSet() {
		map = new IdentityHashMap<E, Object>();
	}

	/**
	 * Constructs a new set containing the elements in the specified collection.
	 * The <tt>HashMap</tt> is created with default load factor (0.75) and an
	 * initial capacity sufficient to contain the elements in the specified
	 * collection.
	 *
	 * @param c
	 *            the collection whose elements are to be placed into this set
	 * @throws NullPointerException
	 *             if the specified collection is null
	 */
	public IdentityHashSet(Collection<? extends E> c) {
		map = new IdentityHashMap<E, Object>(Math.max(
				(int) (c.size() / .75f) + 1, 16));
		addAll(c);
	}

	/**
	 * Constructs a new, empty set; the backing <tt>HashMap</tt> instance has
	 * the specified initial capacity and default load factor (0.75).
	 *
	 * @param initialCapacity
	 *            the initial capacity of the hash table
	 * @throws IllegalArgumentException
	 *             if the initial capacity is less than zero
	 */
	public IdentityHashSet(int initialCapacity) {
		map = new IdentityHashMap<E, Object>(initialCapacity);
	}

	/**
	 * Returns an iterator over the elements in this set. The elements are
	 * returned in no particular order.
	 *
	 * @return an Iterator over the elements in this set
	 * @see java.util.ConcurrentModificationException
	 */
	@Override
	public Iterator<E> iterator() {
		return map.keySet().iterator();
	}

	/**
	 * Returns the number of elements in this set (its cardinality).
	 *
	 * @return the number of elements in this set (its cardinality)
	 */
	@Override
	public int size() {
		return map.size();
	}

	/**
	 * Returns <tt>true</tt> if this set contains no elements.
	 *
	 * @return <tt>true</tt> if this set contains no elements
	 */
	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * Returns <tt>true</tt> if this set contains the specified element. More
	 * formally, returns <tt>true</tt> if and only if this set contains an
	 * element <tt>e</tt> such that
	 * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
	 *
	 * @param o
	 *            element whose presence in this set is to be tested
	 * @return <tt>true</tt> if this set contains the specified element
	 */
	@Override
	public boolean contains(Object o) {
		return map.containsKey(o);
	}

	/**
	 * Adds the specified element to this set if it is not already present. More
	 * formally, adds the specified element <tt>e</tt> to this set if this set
	 * contains no element <tt>e2</tt> such that
	 * <tt>(e==null&nbsp;?&nbsp;e2==null&nbsp;:&nbsp;e.equals(e2))</tt>. If
	 * this set already contains the element, the call leaves the set unchanged
	 * and returns <tt>false</tt>.
	 *
	 * @param e
	 *            element to be added to this set
	 * @return <tt>true</tt> if this set did not already contain the specified
	 *         element
	 */
	@Override
	public boolean add(E e) {
		return map.put(e, PRESENT) == null;
	}

	/**
	 * Removes the specified element from this set if it is present. More
	 * formally, removes an element <tt>e</tt> such that
	 * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>, if
	 * this set contains such an element. Returns <tt>true</tt> if this set
	 * contained the element (or equivalently, if this set changed as a result
	 * of the call). (This set will not contain the element once the call
	 * returns.)
	 *
	 * @param o
	 *            object to be removed from this set, if present
	 * @return <tt>true</tt> if the set contained the specified element
	 */
	@Override
	public boolean remove(Object o) {
		return map.remove(o) == PRESENT;
	}

	/**
	 * Removes all of the elements from this set. The set will be empty after
	 * this call returns.
	 */
	@Override
	public void clear() {
		map.clear();
	}

}
