package com.github.rmannibucau.cookit.api.recipe;

import com.github.rmannibucau.cookit.spi.Container;

import java.io.IOException;
import java.util.Iterator;
import java.util.ServiceLoader;

public interface Recipes {
    static <T extends Recipe> T cook(final Container container, final Class<T> recipe) {
        if (container == null) {
            throw new IllegalArgumentException("this method can be called only when a recipe is already running");
        }

        try {
            final T instance = recipe.newInstance();
            instance.setContainer(container);
            instance.execute();
            return instance;
        } catch (final IllegalAccessException | InstantiationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static <T extends Recipe> T cook(final Class<T> recipe) {
        final Iterator<Container> iterator = ServiceLoader.load(Container.class).iterator();
        try (final Container c = (iterator.hasNext() ?
                    iterator.next() :
                    Container.class.cast( // fallback, hardcoded to let user override it if needed
                        Recipes.class.getClassLoader().loadClass("com.github.rmannibucau.cookit.impl.container.OWBContainer").newInstance()))
                .start()) {
            return cook(c, recipe);
        } catch (final InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    static void main(final String[] args) {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Pass a recipe as parameter please");
        }
        try {
            final String trim = args[0].trim();
            final Class<?> recipe;
            if (trim.endsWith(".java")) {
                recipe = InternalCompiler.compile(trim);
            } else {
                recipe = Thread.currentThread().getContextClassLoader().loadClass(trim);
            }
            cook(Class.class.cast(recipe));
        } catch (final ClassNotFoundException | IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
