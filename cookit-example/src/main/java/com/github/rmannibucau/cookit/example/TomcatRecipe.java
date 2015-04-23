package com.github.rmannibucau.cookit.example;

import com.github.rmannibucau.cookit.api.environment.Node;
import com.github.rmannibucau.cookit.api.environment.Value;
import com.github.rmannibucau.cookit.api.recipe.Recipe;
import com.github.rmannibucau.cookit.api.recipe.file.EnhancedFileFilter;

import java.io.File;
import java.nio.file.Paths;
import java.util.function.Consumer;
import javax.inject.Inject;

/**
 * Can be executed with:
 * $ java \
 *      -Dtomcat.base=/tmp/tomcat2 -Dtomcat.tmp=/tmp/workr # optional \
 *      -jar cookit-bundle.jar \
 *      cookit-example/src/main/java/com/github/rmannibucau/cookit/example/TomcatRecipe.java
 *
 */
public class TomcatRecipe extends Recipe {
    @Inject
    @Value(key = "tomcat.base", or = "/usr/share/tomcat")
    private String base;

    @Inject
    @Value(key = "tomcat.version", or = "8.0.21")
    private String tomcatVersion;

    @Inject
    @Value(key = "tomcat.tmp", or = "/tmp/tomcat")
    private String tmpDir;

    @Inject
    private Node node;

    @Override
    public void configure() {
        id("tomcat");
        set("cookit.maven.localRepository", Paths.get(tmpDir, "m2"));
    }

    @Override
    public void recipe() {
        rmOnExit(tmpDir);

        // download and unzip tomcat
        final File zip = mvn("org.apache.tomcat:tomcat:zip:" + tomcatVersion);
        unzip(zip, base, true);

        // make scripts executables
        final Consumer<File> setExecutable = f -> f.setExecutable(true);
        final EnhancedFileFilter<String> filterByExtension = (f, e) -> f.getName().endsWith(e);
        switch (node.family()) {
            case "windows":
                withFile(base, "bin")
                    .filter(filterByExtension, ".bat")
                    .forEach(setExecutable);
            default:
                withFile(base, "bin")
                    .filter(filterByExtension, ".sh")
                    .forEach(setExecutable);
        }
    }
}
