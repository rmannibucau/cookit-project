package com.github.rmannibucau.cookit.maven.recipe;

import com.github.rmannibucau.cookit.api.environment.Value;
import com.github.rmannibucau.cookit.api.recipe.Recipe;
import com.github.rmannibucau.cookit.maven.resolver.MavenResolver;

import java.io.File;
import java.util.List;
import java.util.Properties;
import javax.inject.Inject;

public class MavenRecipe extends Recipe {
    @Inject
    @Value("cookit.maven.artifacts")
    private List<String> artifacts;

    @Inject
    @Value
    private Properties configuration;

    @Inject
    private MavenResolver mvn;

    @Override
    public void recipe() {
        if (artifacts == null) {
            return;
        }

        artifacts.stream().forEach(a -> {
            final String target = configuration.getProperty(a + ".target");
            final File artifact = mvn.resolve(configuration.getProperty(a + ".coords", a), configuration.getProperty(a + ".repository"));
            if (target != null) {
                cp(artifact.getAbsolutePath(), target);
            }
        });
    }
}
