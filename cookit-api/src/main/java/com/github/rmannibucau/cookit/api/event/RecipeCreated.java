package com.github.rmannibucau.cookit.api.event;

import com.github.rmannibucau.cookit.api.recipe.Recipe;

@Event
public class RecipeCreated {
    private final Recipe recipe;

    public RecipeCreated(final Recipe builder) {
        this.recipe = builder;
    }

    public Recipe getRecipe() {
        return recipe;
    }
}
