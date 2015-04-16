package com.github.rmannibucau.cookit.api.recipe;

import com.github.rmannibucau.cookit.api.recipe.file.EnhancedFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class EnhancedFile {
    private final File delegate;

    EnhancedFile(File delegate) {
        this.delegate = delegate;
    }

    public Stream<File> filter(final FileFilter filter) {
        final File[] files = delegate.listFiles(filter);
        return files != null ? asList(files).stream() : Stream.empty();
    }

    public <T> Stream<File> filter(final EnhancedFileFilter<T> filter, final T arg) {
        final File[] files = delegate.listFiles(f -> filter.accept(f, arg));
        return files != null ? asList(files).stream() : Stream.empty();
    }

    public File getDelegate() {
        return delegate;
    }
}
