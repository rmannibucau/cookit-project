package com.github.rmannibucau.cookit.impl.recipe;

import com.github.rmannibucau.cookit.api.configuration.ConfigurationProvider;
import com.github.rmannibucau.cookit.api.environment.Environment;
import com.github.rmannibucau.cookit.api.environment.Node;
import com.github.rmannibucau.cookit.api.recipe.Recipe;
import com.github.rmannibucau.cookit.impl.configuration.RawConfiguration;
import com.github.rmannibucau.cookit.impl.environment.NodeImpl;
import com.github.rmannibucau.cookit.spi.RecipeProvider;
import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

@Dependent
public class RecipeProviderImpl implements RecipeProvider {
    @Produces
    private static volatile RawConfiguration configuration;

    @Produces
    @Environment(Environment.Property.ID)
    private static volatile String id;

    @Override
    public Recipe newRecipe(final String id,
                            final Collection<String> configurationPaths,
                            final Collection<ConfigurationProvider> configurationProviders,
                            final Map<String, Object> inMemoryConfiguration,
                            final Collection<Runnable> tasks) {
        RecipeProviderImpl.id = id;
        final boolean configurationHolder = configuration == null;
        configuration = new RawConfiguration(buildConfiguration(new NodeImpl(), configurationPaths, inMemoryConfiguration, configurationProviders));
        return new RecipeImpl(tasks) {
            @Override
            public void cook() {
                try {
                    super.cook();
                } finally {
                    if (configurationHolder) {
                        configuration = null;
                    }
                }
            }
        };
    }

    private Map<String, Object> buildConfiguration(
            final Node node,
            final Collection<String> configurations,
            final Map<String, Object> propertiesConfigurations,
            final Collection<ConfigurationProvider> configurationProviders) {
        final Map<String, Object> aggregatedConfiguration = configuration == null ? new HashMap<>() : configuration.getMap();
        aggregatedConfiguration.putAll(propertiesConfigurations);
        configurations.stream().forEach(it -> {
            try (final InputStream is = it.startsWith("classpath:") ?
                    Thread.currentThread().getContextClassLoader().getResourceAsStream(it.substring("classpath:".length())) :
                    new FileInputStream(it)) {
                final Properties p = new Properties();
                p.load(is);
                p.entrySet().stream().forEach(e -> aggregatedConfiguration.put(e.getKey().toString(), e.getValue()));
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
        });
        configurationProviders.stream().forEach(it -> it.provide(aggregatedConfiguration, node));

        final StrSubstitutor substitutor = new StrSubstitutor(new StrLookup<String>() {
            @Override
            public String lookup(final String key) {
                final Object o = aggregatedConfiguration.get(key);
                return o != null ? String.valueOf(o) : null;
            }
        });

        boolean again;
        do {
            again = false;
            for (final Map.Entry<String, Object> entry : aggregatedConfiguration.entrySet()) {
                final Object original = entry.getValue();
                if (String.class.isInstance(original)) {
                    final String templatized = substitutor.replace(String.valueOf(original));
                    if (!original.equals(templatized)) {
                        entry.setValue(templatized);
                        again = true;
                    }
                }
            }
        } while (again);
        return aggregatedConfiguration;
    }
}
