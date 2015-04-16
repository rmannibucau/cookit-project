package com.github.rmannibucau.cookit.api.recipe;

import com.github.rmannibucau.cookit.api.environment.Node;

@FunctionalInterface
public interface ConfigurationProvider {
    void provide(Node node);
}
