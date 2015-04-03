package com.github.rmannibucau.cookit.impl.thread;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;

public class ThreadSafeWrapper<T> {
    private final ThreadLocal<LinkedList<T>> threadLocal = new ThreadLocal<LinkedList<T>>() {
        @Override
        protected LinkedList<T> initialValue() {
            return new LinkedList<>();
        }
    };
    private final AtomicReference<T> fallback = new AtomicReference<>();

    public void set(final T t) {
        threadLocal.get().add(t);
        fallback.compareAndSet(null, t);
    }

    public void reset() {
        final LinkedList<T> list = threadLocal.get();
        final T t = list.removeLast();
        if (list.isEmpty()) {
            threadLocal.remove();
        }
        fallback.compareAndSet(t, null);
    }

    public T get() {
        final T t = getLast();
        if (t != null) {
            return t;
        }
        threadLocal.remove();
        return fallback.get();
    }

    private T getLast() {
        final LinkedList<T> list = threadLocal.get();
        return list.isEmpty() ? null : list.getLast();
    }
}
