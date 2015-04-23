package com.github.rmannibucau.cookit.api.recipe;

import com.github.rmannibucau.cookit.api.recipe.dependency.Maven;
import com.github.rmannibucau.cookit.api.task.FileTask;
import com.github.rmannibucau.cookit.spi.Container;
import com.github.rmannibucau.cookit.spi.RecipeLifecycle;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;

// inherited by Recipe to have task methods
abstract class RecipeBase {
    private String id = "cookit-recipe";
    private final Collection<String> configurations = new LinkedList<>();
    private final Collection<NoArgConfigurationProvider> noArgConfigurationProviders = new LinkedList<>();
    private final Collection<ConfigurationProvider> configurationProviders = new LinkedList<>();
    private final Collection<ConfigurationProviderWithConfiguration> configurationProvidersWithConfiguration = new LinkedList<>();
    private final Map<String, Object> propertiesConfigurations = new HashMap<>();

    private boolean configured = false;

    /**
     * Method intended to be called before configuration injection is complete.
     * It can be used to add configuration to the system.
     */
    public void configure() {
        // no-op
    }

    /**
     * Cook it! Code your recipe here.
     */
    public abstract void recipe();

    public abstract Container container();

    protected void setContainer(final Container container) {
        // no-op
    }

    protected void include(final Class<? extends Recipe> recipe) {
        Recipes.cook(container(), recipe);
    }

    protected RecipeBase id(final String id) {
        assertNotConfigured();
        this.id = id;
        return this;
    }

    protected void configurationFile(final String path) {
        assertNotConfigured();
        this.configurations.add(path);
    }

    protected <T> void configuration(final String key, final T value) {
        assertNotConfigured();
        this.propertiesConfigurations.put(key, value);
    }

    protected void configuration(final ConfigurationProvider provider) {
        assertNotConfigured();
        this.configurationProviders.add(provider);
    }

    protected void configuration(final ConfigurationProviderWithConfiguration provider) {
        assertNotConfigured();
        this.configurationProvidersWithConfiguration.add(provider);
    }

    protected void configuration(final NoArgConfigurationProvider provider) {
        assertNotConfigured();
        this.noArgConfigurationProviders.add(provider);
    }

    private void assertNotConfigured() {
        if (configured) {
            throw new IllegalStateException("can't call configuration method once configure() method was called, ie not in recipe()");
        }
    }

    protected  <T> void set(final String key, final T value) {
        assertNotConfigured();
        container().configuration().put(key, value);
    }

    protected void task(final Runnable task) {
        task.run();
    }

    protected String basic(final String user, final String password) {
        return printBase64Binary((user + ":" + password).getBytes());
    }

    protected EnhancedFile withFile(final String first, final String... segments) {
        return new EnhancedFile(Paths.get(first, segments).toFile());
    }

    protected void unzip(final File from, final String to, final boolean stripRoot) {
        unzip(from.getAbsolutePath(), to, stripRoot);
    }

    protected void unzip(final String from, final String to, final boolean stripRoot) {
        final File source = new File(from);
        if (!source.isFile()) {
            throw new IllegalArgumentException(to + " is not a zip");
        }

        final File target = new File(to);
        target.mkdirs();

        final byte[] buffer = new byte[1024];
        try (final FileInputStream fis = new FileInputStream(source);
             final ZipInputStream zis = new ZipInputStream(fis)) {

            ZipEntry it = zis.getNextEntry();
            while (it != null) {
                String name = it.getName();
                try {
                    if (stripRoot) {
                        int slashIdx = name.indexOf('/');
                        if (slashIdx < 0) {
                            slashIdx = name.indexOf('\\');
                        }
                        if (slashIdx > 0) {
                            name = name.substring(slashIdx + 1);
                        }
                    }
                    if (name.isEmpty()) {
                        continue;
                    }

                    final File newFile = new File(target, name);
                    if (it.isDirectory()) {
                        newFile.mkdirs();
                    } else {
                        newFile.getParentFile().mkdirs();
                        try (FileOutputStream fos = new FileOutputStream(newFile)) {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                } finally {
                    zis.closeEntry();
                    it = zis.getNextEntry();
                }
            }
            zis.closeEntry();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void get(final String url, final String to) {
        get(url, to, null, Proxy.NO_PROXY);
    }

    protected void get(final String url, final String to, final String token, final Proxy proxy) {
        try {
            try (final FileOutputStream fos = new FileOutputStream(to)) {
                doGet(url, fos, token, proxy);
            }
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void get(final String url, final OutputStream to, final String token, final Proxy proxy) {
        doGet(url, to, token, proxy);
    }

    /**
     * NOTE this method is synchronous and should be used in a task()
     */
    protected String syncGet(final String url) {
        return syncGet(url, null, Proxy.NO_PROXY);
    }

    /**
     * NOTE this method is synchronous and should be used in a task()
     */
    protected String syncGet(final String url, final String token, final Proxy proxy) {
        final ByteArrayOutputStream to = new ByteArrayOutputStream();
        doGet(url, to, token, proxy);
        return new String(to.toByteArray());
    }

    private void doGet(final String url, final OutputStream to, final String token, final Proxy proxy) {
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

    protected void rmOnExit(final String path) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                final File f = new File(path);
                if (f.isDirectory()) {
                    rmDir(path);
                } else if (f.exists()) {
                    rm(path);
                }
            }
        });
    }

    protected void ln(final String target, final String link) {
        try {
            final Path targetPath = Paths.get(link);
            if (targetPath.toFile().exists()) {
                rm(link);
            }
            Files.createSymbolicLink(targetPath, Paths.get(target));
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

    protected File mvn(final String coordinates) {
        return container().lookup(Maven.class).resolve(coordinates);
    }

    /**
     * NOTE this method is synchronous and should be used in a task()
     */
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
            CookitFiles.rmDir(dir);
    }

    protected void mkdirs(final String directory) {
        new File(directory).mkdirs();
    }

    protected void withDirectory(final String directory, final FileTask task) {
            final File dir = new File(directory);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IllegalStateException("Directory " + directory + " can't be created");
            }
            task.work(dir);
    }

    protected void withFile(final String file, final FileTask task) {
        task.work(new File(file));
    }

    public void execute() {
        final Iterator<RecipeLifecycle> iterator = ServiceLoader.load(RecipeLifecycle.class).iterator();
        try {
            final RecipeLifecycle recipeLifecycle = iterator.hasNext() ? // let it be overridable
                    iterator.next() :
                    RecipeLifecycle.class.cast(Thread.currentThread().getContextClassLoader().loadClass("com.github.rmannibucau.cookit.impl.recipe.RecipeLifecycleImpl").newInstance());
            recipeLifecycle.run(Recipe.class.cast(this));
        } catch (final IllegalAccessException | ClassNotFoundException | InstantiationException e) {
            throw new IllegalStateException(e);
        }
    }

    public void run() {
        final Class<? extends Recipe> builder = Class.class.cast(getClass()); // base is forbidden, user can only use builder
        Recipes.cook(builder);
    }

    public void configured() {
        configured = true;
    }

    public String getId() {
        return id;
    }

    public Collection<String> getConfigurations() {
        return configurations;
    }

    public Collection<ConfigurationProvider> getConfigurationProviders() {
        return configurationProviders;
    }

    public Map<String, Object> getPropertiesConfigurations() {
        return propertiesConfigurations;
    }

    public Collection<ConfigurationProviderWithConfiguration> getConfigurationProvidersWithConfiguration() {
        return configurationProvidersWithConfiguration;
    }

    public Collection<NoArgConfigurationProvider> getNoArgConfigurationProviders() {
        return noArgConfigurationProviders;
    }
}
