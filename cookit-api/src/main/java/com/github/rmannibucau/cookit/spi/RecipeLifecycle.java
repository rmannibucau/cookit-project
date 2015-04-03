package com.github.rmannibucau.cookit.spi;

import com.github.rmannibucau.cookit.api.recipe.Recipe;

public interface RecipeLifecycle {
    void run(Recipe builder);
}
