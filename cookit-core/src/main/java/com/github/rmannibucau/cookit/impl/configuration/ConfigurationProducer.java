package com.github.rmannibucau.cookit.impl.configuration;

import com.github.rmannibucau.cookit.api.environment.Value;
import com.github.rmannibucau.cookit.api.event.RecipeConfigured;
import com.github.rmannibucau.cookit.api.event.RecipeCreated;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import static java.util.Arrays.asList;

@Dependent
public class ConfigurationProducer {
    private static final Logger LOGGER = Logger.getLogger(ConfigurationProducer.class.getName());
    public static volatile boolean silent = false;

    @Inject
    private RawConfiguration configuration;


    public void silent(@Observes final RecipeCreated configured) {
        silent = true;
    }

    public void noisy(@Observes final RecipeConfigured configured) {
        silent = false;
    }

    @Produces
    @Value("")
    public String str(final InjectionPoint ip) {
        final Value annotation = ip.getAnnotated().getAnnotation(Value.class);
        final String key = annotation.value().isEmpty() ? annotation.key() : annotation.value();

        final Object orDefault;
        if (configuration == null) {
            if (silent) {
                LOGGER.log(silent ? Level.FINER : Level.WARNING,
                        "Using @Value before container is started (ie in builder#configure but not a lambda), configuration is ignored then. " +
                                "You can use Provider<X> to avoid it.");
            }
            orDefault = annotation.or();
        } else {
            orDefault = configuration.getMap().getOrDefault(key, annotation.or());
        }

        return System.getProperty(key, orDefault == null || Value.EMPTY.equals(orDefault) ? null : orDefault.toString());
    }

    @Produces
    @Value("" /* ignored */)
    public Properties configuration() {
        final Properties all = new Properties();
        all.putAll(System.getProperties());
        if (configuration != null) {
            all.putAll(configuration.getMap());
        }
        return all;
    }

    @Produces
    @Value("")
    public Optional<String> optional(final InjectionPoint ip) {
        final String str = str(ip);
        return str == null ? null : Optional.of(str);
    }

    @Produces
    @Value("")
    public Double dl(final InjectionPoint ip) {
        final String str = str(ip);
        return str == null ? null : Double.parseDouble(str);
    }

    @Produces
    @Value("")
    public Long lg(final InjectionPoint ip) {
        final String str = str(ip);
        return str == null ? null : Long.parseLong(str);
    }

    @Produces
    @Value("")
    public Integer integer(final InjectionPoint ip) {
        final String str = str(ip);
        return str == null ? null : Integer.parseInt(str);
    }

    @Produces
    @Value("")
    public Boolean bool(final InjectionPoint ip) {
        final String str = str(ip);
        return str == null ? null : Boolean.parseBoolean(str);
    }

    @Produces
    @Value("")
    public URI uri(final InjectionPoint ip) {
        try {
            final String str = str(ip);
            return str == null ? null : new URI(str);
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Produces
    @Value("")
    public URL url(final InjectionPoint ip) {
        try {
            final String str = str(ip);
            return str == null ? null : new URL(str);
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Produces
    @Value("")
    public Class clazz(final InjectionPoint ip) {
        try {
            final String str = str(ip);
            return str == null ? null : Thread.currentThread().getContextClassLoader().loadClass(str);
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Produces
    @Value("")
    public File file(final InjectionPoint ip) {
        final String str = str(ip);
        return str == null ? null : new File(str);
    }

    @Produces
    @Value("")
    public Path path(final InjectionPoint ip) {
        final String str = str(ip);
        return str == null ? null : new File(str).toPath();
    }

    @Produces
    @Value("")
    public Pattern pattern(final InjectionPoint ip) {
        final String str = str(ip);
        return str == null ? null : Pattern.compile(str);
    }

    @Produces
    @Value("")
    public List<String> strArray(final InjectionPoint ip) {
        final String str = str(ip);
        return str == null ? null : asList(str(ip).split(" *, *"));
    }
}
