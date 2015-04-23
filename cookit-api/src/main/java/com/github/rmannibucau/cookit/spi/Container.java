package com.github.rmannibucau.cookit.spi;

import java.util.Map;

public interface Container extends AutoCloseable {
    Container start();
    boolean isStarted();
    <T> T inject(T instance);

    @Override
    void close();

    Object[] createParameters(Object lambda);

    Map<String, Object> configuration();

    void fire(Object event);

    <T> T lookup(Class<T> service);
}
