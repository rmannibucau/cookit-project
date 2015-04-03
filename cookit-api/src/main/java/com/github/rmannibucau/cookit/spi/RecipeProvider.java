package com.github.rmannibucau.cookit.spi;

import com.github.rmannibucau.cookit.api.recipe.Recipe;
import com.github.rmannibucau.cookit.api.configuration.ConfigurationProvider;

import java.util.Collection;
import java.util.Map;

public interface RecipeProvider {
    Recipe newRecipe(String id,
                     Collection<String> configurationPaths,
                     Collection<ConfigurationProvider> configurationProviders,
                     Map<String, Object> inMemoryConfiguration,
                     Collection<Runnable> tasks);
}
