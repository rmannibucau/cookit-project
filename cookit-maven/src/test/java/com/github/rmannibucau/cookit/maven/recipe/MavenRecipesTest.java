package com.github.rmannibucau.cookit.maven.recipe;

import com.github.rmannibucau.cookit.api.recipe.Recipe;
import com.github.rmannibucau.cookit.api.recipe.Recipes;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MavenRecipesTest {
    private static final File WORK_DIR = new File("target/MavenRecipeTest/");
    static {
        if (WORK_DIR.exists()) {
            FileUtils.deleteQuietly(WORK_DIR);
        }
        WORK_DIR.mkdirs();
    }

    @Test
    public void run() {
        Recipes.cook(Mvn.class);
        assertEquals(2, FileUtils.listFiles(
                new File(WORK_DIR, "m2"),
                    new OrFileFilter(new SuffixFileFilter(".jar"), new SuffixFileFilter(".war")),
                    DirectoryFileFilter.INSTANCE).stream()
                .filter(File::isFile)
                .count());
        assertTrue(new File(WORK_DIR, "lang3.jar").exists());
    }

    public static class Mvn extends Recipe {
        @Override
        public void configure() {
            configuration("cookit.maven.localRepository", new File(WORK_DIR, "m2").getAbsolutePath());
            configuration("cookit.maven.artifacts", "lang3,org.apache.openejb:tomee-webaccess:war:1.7.1");
            configuration("lang3.coords", "org.apache.commons:commons-lang3:3.3.2");
            configuration("lang3.target", new File(WORK_DIR, "lang3.jar").getAbsolutePath());
        }

        @Override
        public void recipe() {
            include(MavenRecipe.class);
        }
    }
}
