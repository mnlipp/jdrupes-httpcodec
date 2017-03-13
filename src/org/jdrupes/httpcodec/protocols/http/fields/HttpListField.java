/*
 * This file is part of the JDrupes non-blocking HTTP Codec
 * Copyright (C) 2016  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jdrupes.httpcodec.protocols.http.fields;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.jdrupes.httpcodec.types.Converter;

/**
 * An HTTP field value that consists of a list of values separated by 
 * a delimiter. The class provides a "list of field values" view 
 * of the values.
 */
public abstract class HttpListField<T> extends HttpField<List<T>>
	implements List<T>, Cloneable {

	/**
	 * Creates a new object with the given field name and no elements. Note 
	 * that in this
	 * initial state, the field is invalid and no string representation
	 * can be generated. This constructor must be followed by method invocations
	 * that add values.
	 * 
	 * @param name the field name
	 * @param converter the converter for the items
	 */
	protected HttpListField(String name, Converter<List<T>> converter) {
		super(name, new ArrayList<>(), converter);
	}

	/**
	 * Creates a new header field object with the given field name and values.
	 * 
	 * @param name
	 *            the field name
	 * @param items
	 * 			  the items
	 * @param converter 
	 * 			  the converter for the items
	 */
	protected HttpListField(String name, List<T> items, 
			Converter<List<T>> converter) {
		super(name, items, converter);
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.protocols.http.fields.HttpField#clone()
	 */
	@Override
	public HttpListField<T> clone() {
		return (HttpListField<T>)super.clone();
	}

	/* (non-Javadoc)
	 * @see org.jdrupes.httpcodec.protocols.http.fields.HttpField#cloneValue()
	 */
	@Override
	protected List<T> cloneValue() {
		return new ArrayList<>(getValue());
	}
	
	/**
	 * Appends the value to the list of values.
	 * 
	 * @param value the value
	 * @return the field
	 */
	public HttpListField<T> append(T value) {
		getValue().add(value);
		return this;
	}
	
	/**
	 * Appends the value to the list of values if it is not already in the list.
	 * 
	 * @param value the value
	 * @return the field
	 */
	public HttpListField<T> appendIfNotContained(T value) {
		if (!getValue().contains(value)) {
			getValue().add(value);
		}
		return this;
	}
	
	/**
	 * Combine this list with another list of the same type.
	 * 
	 * @param other the other list
	 */
	@SuppressWarnings("unchecked")
	public void combine(@SuppressWarnings("rawtypes") HttpListField other) {
		if (!(getClass().equals(other.getClass()))
				|| !getName().equals(other.getName())) {
			throw new IllegalArgumentException("Types and name must be equal.");
		}
		addAll(other);
	}

	/**
	 * @see java.util.List#add(int, java.lang.Object)
	 */
	public void add(int index, T element) {
		getValue().add(index, element);
	}

	/**
	 * @see java.util.List#add(java.lang.Object)
	 */
	public boolean add(T element) {
		return getValue().add(element);
	}

	/**
	 * @see java.util.List#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection<? extends T> collection) {
		return getValue().addAll(collection);
	}

	/**
	 * @see java.util.List#addAll(int, java.util.Collection)
	 */
	public boolean addAll(int index, Collection<? extends T> collection) {
		return getValue().addAll(index, collection);
	}

	/**
	 * @see java.util.List#clear()
	 */
	public void clear() {
		getValue().clear();
	}

	/**
	 * @see java.util.List#contains(java.lang.Object)
	 */
	public boolean contains(Object object) {
		return getValue().contains(object);
	}

	/**
	 * @see java.util.List#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection<?> collection) {
		return getValue().containsAll(collection);
	}

	/**
	 * @see java.util.List#get(int)
	 */
	public T get(int index) {
		return getValue().get(index);
	}

	/**
	 * @see java.util.List#indexOf(java.lang.Object)
	 */
	public int indexOf(Object object) {
		return getValue().indexOf(object);
	}

	/**
	 * @see java.util.List#isEmpty()
	 */
	public boolean isEmpty() {
		return getValue().isEmpty();
	}

	/**
	 * @see java.util.List#iterator()
	 */
	public Iterator<T> iterator() {
		return getValue().iterator();
	}

	/**
	 * @see java.util.List#lastIndexOf(java.lang.Object)
	 */
	public int lastIndexOf(Object object) {
		return getValue().lastIndexOf(object);
	}

	/**
	 * @see java.util.List#listIterator()
	 */
	public ListIterator<T> listIterator() {
		return getValue().listIterator();
	}

	/**
	 * @see java.util.List#listIterator(int)
	 */
	public ListIterator<T> listIterator(int index) {
		return getValue().listIterator(index);
	}

	/**
	 * @see java.util.List#remove(int)
	 */
	public T remove(int index) {
		return getValue().remove(index);
	}

	/**
	 * @see java.util.List#remove(java.lang.Object)
	 */
	public boolean remove(Object object) {
		return getValue().remove(object);
	}

	/**
	 * @see java.util.List#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection<?> collection) {
		return getValue().removeAll(collection);
	}

	/**
	 * @see java.util.List#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection<?> collection) {
		return getValue().retainAll(collection);
	}

	/**
	 * @see java.util.List#set(int, java.lang.Object)
	 */
	public T set(int index, T element) {
		return getValue().set(index, element);
	}

	/**
	 * @see java.util.List#size()
	 */
	public int size() {
		return getValue().size();
	}

	/**
	 * @see java.util.List#subList(int, int)
	 */
	public List<T> subList(int fromIndex, int toIndex) {
		return getValue().subList(fromIndex, toIndex);
	}

	/**
	 * @see java.util.List#toArray()
	 */
	public Object[] toArray() {
		return getValue().toArray();
	}

	/**
	 * @see java.util.List#toArray(java.lang.Object[])
	 */
	public <U> U[] toArray(U[] array) {
		return getValue().toArray(array);
	}
}
