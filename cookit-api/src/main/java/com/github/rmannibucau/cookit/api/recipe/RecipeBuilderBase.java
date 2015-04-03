package com.github.rmannibucau.cookit.api.recipe;

import com.github.rmannibucau.cookit.api.configuration.ConfigurationProvider;
import com.github.rmannibucau.cookit.api.task.FileTask;
import com.github.rmannibucau.cookit.spi.Container;
import com.github.rmannibucau.cookit.spi.RecipeProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.ServiceLoader;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;

// inherited by RecipeBuilder to have task methods
abstract class RecipeBuilderBase {
    private String id = "cookit-recipe";
    private final Collection<String> configurations = new LinkedList<>();
    private final Collection<ConfigurationProvider> configurationProviders = new LinkedList<>();
    private final Map<String, Object> propertiesConfigurations = new HashMap<>();
    private final Collection<Runnable> tasks = new LinkedList<>();

    protected abstract void configure();

    protected abstract Container container();

    protected void setContainer(final Container container) {
        // no-op
    }

    protected void include(final Class<? extends RecipeBuilder> recipeBuilder) {
        task(() -> Recipe.cook(container(), recipeBuilder));
    }

    protected RecipeBuilderBase id(final String id) {
        this.id = id;
        return this;
    }

    protected void configurationFile(final String path) {
        this.configurations.add(path);
    }

    protected <T> void configuration(final String key, final T value) {
        this.propertiesConfigurations.put(key, value);
    }

    protected void configuration(final ConfigurationProvider provider) {
        this.configurationProviders.add(provider);
    }

    protected void task(final Runnable task) {
        this.tasks.add(task);
    }

    protected String basic(final String user, final String password) {
        return printBase64Binary((user + ":" + password).getBytes());
    }

    protected void get(final String url, final String to) {
        get(url, to, null, Proxy.NO_PROXY);
    }

    protected String get(final String url) {
        return get(url, null, Proxy.NO_PROXY);
    }

    protected String get(final String url, final String token, final Proxy proxy) {
        final ByteArrayOutputStream to = new ByteArrayOutputStream();
        get(url, to, token, proxy);
        return new String(to.toByteArray());
    }

    protected void get(final String url, final String to, final String token, final Proxy proxy) {
        try {
            try (final FileOutputStream fos = new FileOutputStream(to)) {
                get(url, fos, token, proxy);
            }
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void get(final String url, final OutputStream to, final String token, final Proxy proxy) {
        try {
            final URL u = new URL(url);
            final HttpURLConnection c = HttpURLConnection.class.cast(u.openConnection(proxy));
            try {
                if (token != null) {
                    c.setRequestProperty("Authorization", token);
                }
                int b;
                byte[] buffer = new byte[512];
                final InputStream is = c.getInputStream();
                while ((b = is.read(buffer)) >= 0) {
                    to.write(buffer, 0, b);
                }
            } finally {
                c.disconnect();
            }
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected void cp(final String from, final String to) {
        try {
            final Path target = Paths.get(to);
            target.toFile().getParentFile().mkdirs();
            Files.copy(Paths.get(from), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void rm(final String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected String text(final String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)));
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void write(final String path, final String content) {
        try {
            Files.write(Paths.get(path), content.getBytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void cpDir(final String from, final String to) {
        final Path src = Paths.get(from);
        if (!src.toFile().isDirectory()) {
            throw new IllegalArgumentException(from + " doesnt exist");
        }

        final Path target = Paths.get(to);
        final File file = target.toFile();
        file.mkdirs();
        try {
            Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                    if (!target.equals(dir)) {
                        Files.createDirectories(target.resolve(src.relativize(dir)));
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    Files.copy(file, target.resolve(src.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void rmDir(final String path) {
        final Path dir = Paths.get(path);
        if (!dir.toFile().isDirectory()) {
            return;
        }
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException ex) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void withDirectory(final String directory, final FileTask task) {
        this.tasks.add(() -> {
            final File dir = new File(directory);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IllegalStateException("Directory " + directory + " can't be created");
            }
            task.work(dir);
        });
    }

    protected void withFile(final String file, final FileTask task) {
        this.tasks.add(() -> task.work(new File(file)));
    }

    public Recipe build() {
        final Iterator<RecipeProvider> iterator = ServiceLoader.load(RecipeProvider.class).iterator();
        try {
            return (iterator.hasNext() ? // let it be overridable
                    iterator.next() :
                    RecipeProvider.class.cast(Thread.currentThread().getContextClassLoader().loadClass("com.github.rmannibucau.cookit.impl.recipe.RecipeProviderImpl").newInstance()))
                        .newRecipe(id, configurations, configurationProviders, propertiesConfigurations, tasks);
        } catch (final IllegalAccessException | ClassNotFoundException | InstantiationException e) {
            throw new IllegalStateException(e);
        }
    }

    public void run() {
        final Class<? extends RecipeBuilder> builder = Class.class.cast(getClass()); // base is forbidden, user can only use builder
        Recipe.cook(builder);
    }
}
