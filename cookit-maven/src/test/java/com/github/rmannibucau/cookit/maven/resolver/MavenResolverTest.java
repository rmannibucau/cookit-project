package com.github.rmannibucau.cookit.maven.resolver;

import com.github.rmannibucau.cookit.api.recipe.Recipe;
import com.github.rmannibucau.cookit.api.recipe.Recipes;
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
        Recipes.cook(JarRecipe.class);
        assertNotNull(JarRecipe.lang3);
        assertEquals(
            new File(WORK_DIR, "org/apache/commons/commons-lang3/3.3.2/commons-lang3-3.3.2.jar").getCanonicalFile(),
            JarRecipe.lang3.getCanonicalFile());
    }

    @Test
    public void resolveWar() throws IOException {
        Recipes.cook(WarRecipe.class);
        assertNotNull(WarRecipe.webaccess);
        assertEquals(
                new File(WORK_DIR, "org/apache/openejb/tomee-webaccess/1.7.1/tomee-webaccess-1.7.1.war").getCanonicalFile(),
                WarRecipe.webaccess.getCanonicalFile());
    }

    public static class ConfigRecipe extends Recipe {
        @Override
        public void configure() {
            configuration("cookit.maven.localRepository", WORK_DIR.getAbsolutePath());
        }

        @Override
        public void recipe() {
            // no-op
        }
    }

    public static class JarRecipe extends Recipe {
        private static File lang3;

        @Override
        public void recipe() {
            include(ConfigRecipe.class);
            task((MavenResolver mvn) -> {
                lang3 = mvn.resolve("org.apache.commons:commons-lang3:3.3.2");
            });
        }
    }

    public static class WarRecipe extends Recipe {
        private static File webaccess;

        @Override
        public void recipe() {
            include(ConfigRecipe.class);
            task((MavenResolver mvn) -> {
                webaccess = mvn.resolve("org.apache.openejb:tomee-webaccess:war:1.7.1");
            });
        }
    }
}
