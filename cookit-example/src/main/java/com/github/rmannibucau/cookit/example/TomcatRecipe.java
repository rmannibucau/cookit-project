package com.github.rmannibucau.cookit.example;

import com.github.rmannibucau.cookit.api.environment.Node;
import com.github.rmannibucau.cookit.api.environment.Value;
import com.github.rmannibucau.cookit.api.recipe.Recipe;
import com.github.rmannibucau.cookit.api.recipe.Recipes;
import com.github.rmannibucau.cookit.maven.recipe.MavenRecipe;

import java.io.File;
import java.nio.file.Paths;
import javax.inject.Inject;

import static java.util.Arrays.asList;

public class TomcatRecipe extends Recipe {
    @Inject
    @Value(key = "tomcat.base", or = "/usr/share/tomcat")
    private String base;

    @Inject
    @Value(key = "tomcat.version", or = "8.0.20")
    private String tomcatVersion;

    @Inject
    @Value(key = "tomcat.tmp", or = "/tmp/tomcat")
    private String tmpDir;

    @Inject
    @Value(key = "tomcat.target")
    private String zip;

    @Inject
    private Node node;

    @Override
    public void configure() {
        id("tomcat");

        configuration(() -> {
            set("cookit.maven.localRepository", Paths.get(tmpDir, "m2"));
            set("cookit.maven.artifacts", "tomcat");
            set("tomcat.coords", "org.apache.tomcat:tomcat:zip:" + tomcatVersion);
            set("tomcat.target", Paths.get(tmpDir, "tomcat-recipe/tomcat.zip").toFile().getAbsolutePath());
        });
    }

    @Override
    public void recipe() {
        include(MavenRecipe.class);

        // then create the structure
        unzip(zip, base, true);

        // make scripts executables
        switch (node.family()) {
            case "windows":
                asList(new File(base, "bin").listFiles(f -> f.getName().endsWith(".sh")))
                    .stream()
                    .forEach(f -> f.setExecutable(true));
            default:
                asList(new File(base, "bin").listFiles(f -> f.getName().endsWith(".bat")))
                        .stream()
                        .forEach(f -> f.setExecutable(true));
        }

        // clean up
        rmDir(tmpDir);
    }

    public static void main(final String[] args) {
        // target/ for test
        System.setProperty("tomcat.base", "target/test/tomcat");

        // run it!
        Recipes.cook(TomcatRecipe.class);
    }
}
