package com.github.rmannibucau.cookit.api.environment;

public interface Node {
    /**
     * @return [x86_64, x86, i386, ppc, sparc, PowerPC, arm, ...]
     */
    String arch();

    /**
     * @return [Linux,...]
     */
    String name();

    /**
     * @return os version, on linux it can look like "3.13.0-48-generic"
     */
    String version();

    /**
     * @return [rhel, fedora, ubuntu, debian, gentoo, ...]
     */
    String family();

    boolean isAix();
    boolean isLinux();
    boolean isMac();
    boolean isUNIX();
    boolean isWindows();
}
