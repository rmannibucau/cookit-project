package com.github.rmannibucau.cookit.api.recipe.dependency;

import java.io.File;

public interface Maven {
    File resolve(final String coords, final String repository);

    default File resolve(final String coords) {
        return resolve(coords, null);
    }
}
