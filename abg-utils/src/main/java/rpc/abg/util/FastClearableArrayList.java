package rpc.abg.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * ArrayList 的简化版，可快速 clear，非通用仅适用于特定场景
 * 
 * @author Annabergite
 *
 * @param <E>
 */
public class FastClearableArrayList<E> implements List<E>, RandomAccess, Cloneable {

	private Object[] data;
	private int size;

	public FastClearableArrayList() {
		this(8);
	}

	public FastClearableArrayList(int initialCapacity) {
		if (initialCapacity > 0) {
			this.data = new Object[TableUtils.tableSizeFor(initialCapacity)];
		} else {
			throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
		}
	}

	public void ensureCapacity(int minCapacity) {
		if (minCapacity > data.length) {
			data = Arrays.copyOf(data, TableUtils.tableSizeFor(minCapacity));
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public E get(int index) {
		Objects.checkIndex(index, size);

		return (E) data[index];
	}

	@Override
	public boolean add(E e) {
		int index = size;
		ensureCapacity(++size);
		data[index] = e;

		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public E set(int index, E element) {
		Objects.checkIndex(index, size);

		E oldValue = (E) data[index];
		data[index] = element;
		return oldValue;
	}

	@Override
	public void clear() {
		size = 0;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public Object[] toArray() {
		return data;
	}

	@Override
	public boolean contains(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<E> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(int index, E element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public E remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int indexOf(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int lastIndexOf(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<E> listIterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}

}
