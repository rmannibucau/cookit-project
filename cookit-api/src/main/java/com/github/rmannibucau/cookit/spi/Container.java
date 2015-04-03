package com.github.rmannibucau.cookit.spi;

public interface Container extends AutoCloseable {
    Container start();
    boolean isStarted();
    <T> T inject(T instance);

    @Override
    void close();

    Object[] createParameters(Object lambda);
}
