package com.github.rmannibucau.cookit.impl.environment;

import com.github.rmannibucau.cookit.api.environment.Node;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

// let it be a cdi bean as well if needed
public class NodeImpl implements Node {
    private String family;

    @Override
    public String arch() {
        return SystemUtils.OS_ARCH;
    }

    @Override
    public String name() {
        return SystemUtils.OS_NAME;
    }

    @Override
    public String version() {
        return SystemUtils.OS_VERSION;
    }

    @Override
    public String family() {
        return System.getProperty("cookit.node.family", family != null ? family : (family = findFamily()));
    }

    @Override
    public boolean isAix() {
        return SystemUtils.IS_OS_AIX;
    }

    @Override
    public boolean isLinux() {
        return SystemUtils.IS_OS_LINUX;
    }

    @Override
    public boolean isMac() {
        return SystemUtils.IS_OS_MAC;
    }

    @Override
    public boolean isUNIX() {
        return SystemUtils.IS_OS_UNIX;
    }

    @Override
    public boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    private synchronized String findFamily() {
        if (isWindows()) {
            return "windows";
        }

        final File osRelease = new File("/etc/os-release");
        if (osRelease.isFile() && osRelease.canRead()) {
            final Properties file = new Properties();
            try (final InputStream is = new FileInputStream(osRelease)) {
                file.load(is);
            } catch (final IOException e) {
                // no-op
            }
            if (file.containsKey("ID")) {
                return file.getProperty("ID");
            }
            if (file.containsKey("NAME")) {
                return file.getProperty("NAME").toLowerCase(Locale.ENGLISH);
            }
            // else let use generic strings
        }

        if (isLinux()) {
            return "linux";
        }
        if (isMac()) {
            return "max";
        }
        if (isAix()) {
            return "aix";
        }
        return "unknown";
    }
}
