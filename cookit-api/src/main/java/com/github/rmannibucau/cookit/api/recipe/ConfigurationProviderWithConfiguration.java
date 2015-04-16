package com.github.rmannibucau.cookit.api.recipe;

import com.github.rmannibucau.cookit.api.environment.Node;

import java.util.Map;

@FunctionalInterface
public interface ConfigurationProviderWithConfiguration {
    void provide(Map<String, Object> current, Node node);
}
