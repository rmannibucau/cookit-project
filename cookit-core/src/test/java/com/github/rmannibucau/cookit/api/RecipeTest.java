package com.github.rmannibucau.cookit.api;

import com.github.rmannibucau.cookit.api.environment.Environment;
import com.github.rmannibucau.cookit.api.environment.Value;
import com.github.rmannibucau.cookit.api.recipe.Recipe;
import com.github.rmannibucau.cookit.api.recipe.RecipeBuilder;
import com.github.rmannibucau.cookit.api.task.CooKitTaskWith1Parameter;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Provider;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class RecipeTest {
    public static Collection<String> SEEN = new LinkedList<>();

    @Before
    public void clean() {
        SEEN.clear();
    }

    @Test
    public void tasks() {
        Recipe.cook(TaskRecipe.class);
        assertEquals(4, SEEN.size());
        assertEquals(asList("task1", "task2", "task3", "task4"), SEEN);
    }

    @Test
    public void files() {
        Recipe.cook(FileRecipe.class);
        assertEquals(2, SEEN.size());
        assertEquals(asList("task5", "task6"), SEEN);
    }

    @Test
    public void aggregation() {
        Recipe.cook(Orchestration.class);
        assertEquals(6, SEEN.size());
        assertEquals(asList("task1", "task2", "task3", "task4", "task5", "task6"), SEEN);
    }

    @Test
    public void io() {
        Recipe.cook(IORecipe.class);
    }

    @Test
    public void classpathConfiguration() {
        Recipe.cook(CPConfiguration.class);
    }

    @Dependent
    public static class ABeanINeed {
        public String doIt() {
            return "done";
        }
    }

    public static class CPConfiguration extends RecipeBuilder {
        @Inject
        @Value("foo.bar")
        private Provider<Integer> fooBar;

        @Override
        protected void configure() {
            id("orchestration");
            configurationFile("classpath:sample.properties");
            task(() -> {
                assertEquals(8080, fooBar.get().intValue());
            });
        }
    }

    public static class Orchestration extends RecipeBuilder {
        @Override
        protected void configure() {
            id("orchestration");
            configuration("v1", "wow");
            include(TaskRecipe.class);
            include(FileRecipe.class);
        }
    }

    public static class TaskRecipe extends RecipeBuilder {
        @Inject
        @Environment(Environment.Property.ID)
        private String id;

        @Inject
        @Value("v1")
        private Provider<String> v1;

        @Inject
        @Value("config")
        private Provider<String> config;

        @Inject
        @Value("number")
        private Provider<Long> number;

        @Override
        protected void configure() {
            id("RecipeTest#cook");

            final AtomicReference<Boolean> checkConfig = new AtomicReference<>(true);
            configuration((current, node) -> {
                if (current.containsKey("v1") && "wow".equals(v1.get())) {
                    checkConfig.set(false);
                    return;
                }
                current.put("v1", "value");
            });
            configuration("config", "pre${v1}suf");
            configuration("number", "1");

            task(() -> {
                SEEN.add("task1");

                if (checkConfig.get()) {
                    assertEquals("prevaluesuf", config.get());
                    assertEquals("RecipeTest#cook", id);
                    assertEquals(1L, number.get().longValue());
                }
            });
            task((ABeanINeed bean) -> {
                SEEN.add("task2");
                assertEquals("done", bean.doIt());
            });
            task((ABeanINeed bean1, ABeanINeed bean2) -> { // note: cannot be replaced by method reference Assert::assertNotSame cause of typing
                SEEN.add("task3");
                assertNotSame(bean1, bean2);
            });
            task(new CooKitTaskWith1Parameter<ABeanINeed>() { // try normal reflection and not lambda which is already tested
                @Override
                public void run(final ABeanINeed param) {
                    SEEN.add("task4");
                    assertEquals("done", param.doIt());
                }
            });
        }
    }

    public static class FileRecipe extends RecipeBuilder {
        @Override
        protected void configure() {
            // file tasks
            withFile("target/RecipeTest/file", f -> {
                SEEN.add("task5");
                try {
                    f.getParentFile().mkdirs();
                    f.createNewFile();
                } catch (final IOException e) {
                    // no-op
                }
                assertTrue(f.exists());
                f.setExecutable(true);
                assertTrue(f.canExecute()); // not sure it works for windows...
            });
            withDirectory("target/RecipeTest/dir", dir -> {
                SEEN.add("task6");
                assertTrue(dir.exists());
                assertTrue(dir.isDirectory());
            });
        }
    }

    public static class IORecipe extends RecipeBuilder {
        @Override
        protected void configure() {
            task(() -> {
                final File file = new File("target/RecipeTest/io/");
                file.mkdirs();
                assertTrue(file.exists());

                rmDir(file.getAbsolutePath());
                assertFalse(file.exists());

                file.mkdirs();
                write(new File(file, "content1").getAbsolutePath(), "c1");
                write(new File(file, "content2").getAbsolutePath(), "c2");
                cpDir(file.getAbsolutePath(), new File(file.getParentFile(), "io2").getAbsolutePath());
                assertTrue(new File(file.getParentFile(), "io2").exists());
                assertTrue(new File(file.getParentFile(), "io2/content1").exists());
                assertTrue(new File(file.getParentFile(), "io2/content2").exists());
                assertEquals("c1", text(new File(file.getParentFile(), "io2/content1").getAbsolutePath()));
                assertEquals("c2", text(new File(file.getParentFile(), "io2/content2").getAbsolutePath()));
                rmDir(new File(file.getParentFile(), "io2").getAbsolutePath());
                assertFalse(new File(file.getParentFile(), "io2").exists());
                rmDir(file.getAbsolutePath());

                assertEquals("864d0989154238f62338083853cedabe", get("http://repo1.maven.org/maven2/org/apache/openejb/openejb/4.7.1/openejb-4.7.1.pom.md5").trim());
                final String md5 = new File("target/RecipeTest/md5").getAbsolutePath();
                get("http://repo1.maven.org/maven2/org/apache/openejb/openejb/4.7.1/openejb-4.7.1.pom.md5", md5);
                assertEquals("864d0989154238f62338083853cedabe", text(md5).trim());
            });
        }
    }
}
