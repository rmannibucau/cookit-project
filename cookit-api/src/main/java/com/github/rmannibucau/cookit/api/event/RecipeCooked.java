package com.github.rmannibucau.cookit.api.event;

import com.github.rmannibucau.cookit.api.recipe.Recipe;

@Event
public class RecipeCooked {
    private final Recipe recipe;

    public RecipeCooked(final Recipe builder) {
        this.recipe = builder;
    }

    public Recipe getRecipe() {
        return recipe;
    }
}
