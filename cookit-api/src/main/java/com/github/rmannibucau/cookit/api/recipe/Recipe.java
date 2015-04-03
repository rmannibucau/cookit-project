package com.github.rmannibucau.cookit.api.recipe;

import com.github.rmannibucau.cookit.spi.Container;

import java.util.Iterator;
import java.util.ServiceLoader;

public interface Recipe {
    void cook();

    static <T extends RecipeBuilder> T cook(final Container container, final Class<T> recipeBuilder) {
        if (container == null) {
            throw new IllegalArgumentException("this method can be called only when a recipe is already running");
        }

        try {
            final T builder = recipeBuilder.newInstance();
            builder.setContainer(container);
            container.inject(builder); // for configure()
            builder.configure();

            final Recipe recipe = builder.build();
            container.inject(builder); // for runtime (reuse just configured properties)
            recipe.cook();
            return builder;
        } catch (final IllegalAccessException | InstantiationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static <T extends RecipeBuilder> T cook(final Class<T> recipeBuilder) {
        final Iterator<Container> iterator = ServiceLoader.load(Container.class).iterator();
        try (final Container c = (iterator.hasNext() ?
                    iterator.next() :
                    Container.class.cast( // fallback, hardcoded to let user override it if needed
                        Recipe.class.getClassLoader().loadClass("com.github.rmannibucau.cookit.impl.container.OWBContainer").newInstance()))
                .start()) {
            return cook(c, recipeBuilder);
        } catch (final InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
