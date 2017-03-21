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

import org.jdrupes.httpcodec.types.ListConverter_1;

/**
 * An HTTP field value that consists of a list of values separated by 
 * a delimiter. The class provides a "list of field values" view 
 * of the values.
 * 
 * @param <I> the type of the items in the list
 */
public abstract class HttpListField<I> extends HttpField<List<I>>
	implements List<I> {

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
	protected HttpListField(String name, ListConverter_1<I> converter) {
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
	protected HttpListField(String name, List<I> items, 
			ListConverter_1<I> converter) {
		super(name, items, converter);
	}

	/**
	 * Returns the cconverter used by this field.
	 * 
	 * @return the converter
	 */
	public ListConverter_1<I> converter() {
		return (ListConverter_1<I>)super.converter();
	}

	/**
	 * Appends the value to the list of values.
	 * 
	 * @param value the value
	 * @return the field
	 */
	public HttpListField<I> append(I value) {
		value().add(value);
		return this;
	}
	
	/**
	 * Appends the value to the list of values if it is not already in the list.
	 * 
	 * @param value the value
	 * @return the field
	 */
	public HttpListField<I> appendIfNotContained(I value) {
		if (!value().contains(value)) {
			value().add(value);
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
				|| !name().equals(other.name())) {
			throw new IllegalArgumentException("Types and name must be equal.");
		}
		addAll(other);
	}

	/**
	 * @see java.util.List#add(int, java.lang.Object)
	 */
	public void add(int index, I element) {
		value().add(index, element);
	}

	/**
	 * @see java.util.List#add(java.lang.Object)
	 */
	public boolean add(I element) {
		return value().add(element);
	}

	/**
	 * @see java.util.List#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection<? extends I> collection) {
		return value().addAll(collection);
	}

	/**
	 * @see java.util.List#addAll(int, java.util.Collection)
	 */
	public boolean addAll(int index, Collection<? extends I> collection) {
		return value().addAll(index, collection);
	}

	/**
	 * @see java.util.List#clear()
	 */
	public void clear() {
		value().clear();
	}

	/**
	 * @see java.util.List#contains(java.lang.Object)
	 */
	public boolean contains(Object object) {
		return value().contains(object);
	}

	/**
	 * @see java.util.List#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection<?> collection) {
		return value().containsAll(collection);
	}

	/**
	 * @see java.util.List#get(int)
	 */
	public I get(int index) {
		return value().get(index);
	}

	/**
	 * @see java.util.List#indexOf(java.lang.Object)
	 */
	public int indexOf(Object object) {
		return value().indexOf(object);
	}

	/**
	 * @see java.util.List#isEmpty()
	 */
	public boolean isEmpty() {
		return value().isEmpty();
	}

	/**
	 * @see java.util.List#iterator()
	 */
	public Iterator<I> iterator() {
		return value().iterator();
	}

	/**
	 * @see java.util.List#lastIndexOf(java.lang.Object)
	 */
	public int lastIndexOf(Object object) {
		return value().lastIndexOf(object);
	}

	/**
	 * @see java.util.List#listIterator()
	 */
	public ListIterator<I> listIterator() {
		return value().listIterator();
	}

	/**
	 * @see java.util.List#listIterator(int)
	 */
	public ListIterator<I> listIterator(int index) {
		return value().listIterator(index);
	}

	/**
	 * @see java.util.List#remove(int)
	 */
	public I remove(int index) {
		return value().remove(index);
	}

	/**
	 * @see java.util.List#remove(java.lang.Object)
	 */
	public boolean remove(Object object) {
		return value().remove(object);
	}

	/**
	 * @see java.util.List#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection<?> collection) {
		return value().removeAll(collection);
	}

	/**
	 * @see java.util.List#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection<?> collection) {
		return value().retainAll(collection);
	}

	/**
	 * @see java.util.List#set(int, java.lang.Object)
	 */
	public I set(int index, I element) {
		return value().set(index, element);
	}

	/**
	 * @see java.util.List#size()
	 */
	public int size() {
		return value().size();
	}

	/**
	 * @see java.util.List#subList(int, int)
	 */
	public List<I> subList(int fromIndex, int toIndex) {
		return value().subList(fromIndex, toIndex);
	}

	/**
	 * @see java.util.List#toArray()
	 */
	public Object[] toArray() {
		return value().toArray();
	}

	/**
	 * @see java.util.List#toArray(java.lang.Object[])
	 */
	public <U> U[] toArray(U[] array) {
		return value().toArray(array);
	}
}
