package com.github.rmannibucau.cookit.maven.recipe;

import com.github.rmannibucau.cookit.api.environment.Value;
import com.github.rmannibucau.cookit.api.recipe.RecipeBuilder;
import com.github.rmannibucau.cookit.maven.resolver.MavenResolver;

import java.io.File;
import java.util.List;
import java.util.Properties;
import javax.inject.Inject;

public class MavenRecipe extends RecipeBuilder {
    @Inject
    @Value("jdt.maven.artifacts")
    private List<String> artifacts;
    @Inject
    @Value
    private Properties configuration;

    @Override
    protected void configure() {
        if (artifacts == null) {
            return;
        }

        task((MavenResolver mvn) -> {
            artifacts.stream().forEach(a -> {
                final String target = configuration.getProperty(a + ".target");
                final File artifact = mvn.resolve(configuration.getProperty(a + ".coords", a), configuration.getProperty(a + ".repository"));
                if (target != null) {
                    cp(artifact.getAbsolutePath(), target);
                }
            });
        });
    }
}
