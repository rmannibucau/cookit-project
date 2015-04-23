package com.github.rmannibucau.cookit.impl.recipe;

import com.github.rmannibucau.cookit.api.environment.Environment;
import com.github.rmannibucau.cookit.api.environment.Node;
import com.github.rmannibucau.cookit.api.event.RecipeConfigured;
import com.github.rmannibucau.cookit.api.event.RecipeCooked;
import com.github.rmannibucau.cookit.api.event.RecipeCreated;
import com.github.rmannibucau.cookit.api.recipe.NoArgConfigurationProvider;
import com.github.rmannibucau.cookit.api.recipe.Recipe;
import com.github.rmannibucau.cookit.impl.configuration.RawConfiguration;
import com.github.rmannibucau.cookit.impl.environment.NodeImpl;
import com.github.rmannibucau.cookit.spi.Container;
import com.github.rmannibucau.cookit.spi.RecipeLifecycle;
import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

@Dependent
public class RecipeLifecycleImpl implements RecipeLifecycle {
    @Produces
    private static volatile RawConfiguration configuration;

    @Produces
    @Environment(Environment.Property.ID)
    private static volatile String id;

    @Override
    public void run(final Recipe builder) {
        Objects.requireNonNull(builder);

        final Container container = builder.container();
        container.fire(new RecipeCreated(builder));

        final boolean configurationHolder = configuration == null;
        configuration = new RawConfiguration(configurationHolder ? new HashMap<>() : configuration.getMap());
        if (configurationHolder) {
            configuration.getMap().putAll(Map.class.cast(System.getProperties()));
        }

        container.inject(builder);
        builder.configure();

        RecipeLifecycleImpl.id = builder.getId(); // not init before configure()
        buildConfiguration(new NodeImpl(), builder);

        try {
            builder.configured();
            container.fire(new RecipeConfigured(builder));

            container.inject(builder); // reinject since now we have configurations, note: we could also do the injection without CDI here
            builder.recipe();
            container.fire(new RecipeCooked(builder));
        } finally {
            if (configurationHolder) {
                configuration = null;
            }
        }
    }

    private void buildConfiguration(final Node node, final Recipe recipe) {
        final Map<String, Object> aggregatedConfiguration = configuration.getMap();

        { // default classpath config based on recipe id
            final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(Optional.of(id).orElse("cookit") + ".properties");
            if (is != null) {
                try {
                    final Properties p = new Properties();
                    p.load(is);
                    p.entrySet().stream().forEach(e -> aggregatedConfiguration.put(e.getKey().toString(), e.getValue()));
                } catch (final IOException e) {
                    throw new IllegalArgumentException(e);
                } finally {
                    try {
                        is.close();
                    } catch (final IOException e) {
                        // no-op
                    }
                }
            }
        }
        aggregatedConfiguration.putAll(recipe.getPropertiesConfigurations());
        recipe.getConfigurations().stream().forEach(it -> {
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
        recipe.getNoArgConfigurationProviders().stream().forEach(NoArgConfigurationProvider::provide);
        recipe.getConfigurationProviders().stream().forEach(it -> it.provide(node));
        recipe.getConfigurationProvidersWithConfiguration().stream().forEach(it -> it.provide(aggregatedConfiguration, node));

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
    }
}
