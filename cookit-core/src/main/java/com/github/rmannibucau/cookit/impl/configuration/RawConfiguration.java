package com.github.rmannibucau.cookit.impl.configuration;

import java.util.Map;

public class RawConfiguration {
    private final Map<String, Object> map;

    public RawConfiguration(final Map<String, Object> stringObjectMap) {
        this.map = stringObjectMap;
    }

    public Map<String, Object> getMap() {
        return map;
    }
}
