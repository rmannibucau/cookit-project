package com.github.rmannibucau.cookit.api.event;

import com.github.rmannibucau.cookit.api.recipe.Recipe;

@Event
public class RecipeConfigured {
    private final Recipe recipe;

    public RecipeConfigured(final Recipe builder) {
        this.recipe = builder;
    }

    public Recipe getRecipe() {
        return recipe;
    }
}
