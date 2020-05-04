/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

public class ImmutableCollectionView<From, To> implements CollectionView<From, To> {

    private Collection<From> collection;

    private Function<From, To> mapper;

    ImmutableCollectionView(Collection<From> collection, Function<From, To> mapper) {
        this.collection = collection;
        this.mapper = mapper;
    }

    @Override
    public int size() {
        return collection.size();
    }

    @Override
    public boolean isEmpty() {
        return collection.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException(
                "contains() currently is not supported");
    }

    @Override
    public Iterator<To> iterator() {
        return new Iterator<To>() {

            private Iterator<From> iter = collection.iterator();

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public To next() {
                return mapper.apply(iter.next());
            }
        };
    }

    @Override
    public Object[] toArray() {
        return collection.stream()
                .map(mapper)
                .toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("ImmutableCollectionView does not support toArray(T[]) yet.");
    }

    @Override
    public boolean add(To to) {
        throw new UnsupportedOperationException("ImmutableCollectionView cannot be modified.");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("ImmutableCollectionView cannot be modified.");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return collection.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends To> c) {
        throw new UnsupportedOperationException("ImmutableCollectionView cannot be modified.");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("ImmutableCollectionView cannot be modified.");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("ImmutableCollectionView cannot be modified.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("ImmutableCollectionView cannot be modified.");
    }
}
