package com.github.rmannibucau.cookit.api.configuration;

import com.github.rmannibucau.cookit.api.environment.Node;

import java.util.Map;

@FunctionalInterface
public interface ConfigurationProvider {
    void provide(Map<String, Object> current, Node node);
}
