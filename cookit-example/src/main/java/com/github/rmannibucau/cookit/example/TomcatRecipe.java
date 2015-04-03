package com.github.rmannibucau.cookit.example;

import com.github.rmannibucau.cookit.api.recipe.Recipe;
import com.github.rmannibucau.cookit.api.recipe.RecipeBuilder;

public class TomcatRecipe extends RecipeBuilder {
    @Override
    protected void configure() {
        configuration((config, node) -> {
            // TODO
            // config.put("jdt.maven.")
        });
    }

    public static void main(String[] args) {
        Recipe.cook(TomcatRecipe.class);
    }
}
