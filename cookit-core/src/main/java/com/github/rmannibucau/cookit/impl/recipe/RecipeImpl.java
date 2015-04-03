package com.github.rmannibucau.cookit.impl.recipe;

import com.github.rmannibucau.cookit.api.recipe.Recipe;

import java.util.Collection;

public class RecipeImpl implements Recipe {
    private final Collection<Runnable> tasks;

    public RecipeImpl(final Collection<Runnable> tasks) {
        this.tasks = tasks;
    }

    @Override
    public void cook() {
        tasks.stream().forEach(Runnable::run);
    }
}
