package com.github.rmannibucau.cookit.api.recipe;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import static java.util.Arrays.asList;

class InternalCompiler {
    // TODO: add workdir, use temp for now
    static Class<?> compile(final String trim) throws IOException, ClassNotFoundException {
        final File f = new File(trim);
        if (!f.isFile()) {
            throw new IllegalArgumentException(trim + " is not a file");
        }

        final Path path = f.toPath();
        String pck = null;
        try {
            Files.lines(path)
                    .forEach(s -> {
                        if (s.startsWith("package ")) { // no need to read the whole file
                            throw new Stop(s.substring("package ".length(), s.indexOf(';')));
                        }
                    });

        } catch (final Stop failFast) {
            pck = failFast.getMessage();
        }

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final Path workDir = Files.createTempDirectory("cookit");
        try {
            final Collection<String> args = new ArrayList<>(
                    asList("-classpath", System.getProperty("surefire.real.class.path", System.getProperty("java.class.path")),
                        "-sourcepath", path.toFile().getParentFile().getAbsolutePath(),
                        "-d", workDir.toFile().getAbsolutePath()));
            asList(f.getParentFile().listFiles(file -> file.getName().endsWith(".java")))
                    .stream()
                    .forEach(it -> args.add(it.getAbsolutePath()));

            compiler.run(null, null, null, args.toArray(new String[args.size()]));
            return new URLClassLoader(new URL[]{ workDir.toUri().toURL() })
                    .loadClass((pck == null ? "" : pck + ".") + f.getName().substring(0, f.getName().lastIndexOf('.')));
        } finally {
            CookitFiles.rmDir(workDir);
        }
    }

    private static class Stop extends RuntimeException {
        private Stop(final String message) {
            super(message);
        }
    }
}
