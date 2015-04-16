package com.github.rmannibucau.cookit.api.recipe.file;

import java.io.File;

@FunctionalInterface
public interface EnhancedFileFilter<T> {
    boolean accept(File pathname, T arg);
}
