package com.github.rmannibucau.cookit.maven.resolver;

import com.github.rmannibucau.cookit.api.recipe.Recipe;
import com.github.rmannibucau.cookit.api.recipe.RecipeBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MavenResolverTest {
    private static final File WORK_DIR = new File("target/MavenResolverTest/");
    static {
        if (WORK_DIR.exists()) {
            FileUtils.deleteQuietly(WORK_DIR);
        }
        WORK_DIR.mkdirs();
    }

    @Test
    public void resolveJar() throws IOException {
        Recipe.cook(JarRecipe.class);
        assertNotNull(JarRecipe.lang3);
        assertEquals(
            new File(WORK_DIR, "org/apache/commons/commons-lang3/3.3.2/commons-lang3-3.3.2.jar").getCanonicalFile(),
            JarRecipe.lang3.getCanonicalFile());
    }

    @Test
    public void resolveWar() throws IOException {
        Recipe.cook(WarRecipe.class);
        assertNotNull(WarRecipe.webaccess);
        assertEquals(
                new File(WORK_DIR, "org/apache/openejb/tomee-webaccess/1.7.1/tomee-webaccess-1.7.1.war").getCanonicalFile(),
                WarRecipe.webaccess.getCanonicalFile());
    }

    public static class ConfigRecipe extends RecipeBuilder {
        @Override
        protected void configure() {
            configuration("cookit.maven.localRepository", WORK_DIR.getAbsolutePath());
        }
    }

    public static class JarRecipe extends RecipeBuilder {
        private static File lang3;

        @Override
        protected void configure() {
            include(ConfigRecipe.class);
            task((MavenResolver mvn) -> {
                lang3 = mvn.resolve("org.apache.commons:commons-lang3:3.3.2");
            });
        }
    }

    public static class WarRecipe extends RecipeBuilder {
        private static File webaccess;

        @Override
        protected void configure() {
            include(ConfigRecipe.class);
            task((MavenResolver mvn) -> {
                webaccess = mvn.resolve("org.apache.openejb:tomee-webaccess:war:1.7.1");
            });
        }
    }
}
