package org.hihan.joglfx;

import java.io.File;
import java.lang.reflect.Field;

public class Native {

    public enum OS {

        Linux("linux"), MacOSX("mac"), Windows("win");

        String[] names;

        OS(String... names) {
            this.names = names;
        }

        public static OS resolve(String osName) {
            for (OS os : values()) {
                for (String name : os.names) {
                    if (osName.toLowerCase().contains(name)) {
                        return os;
                    }
                }
            }
            return null;
        }
    }

    public static void setLibraryPath() {
        String osName = System.getProperty("os.name");
        OS os = OS.resolve(osName);
        if (os != null) {
            boolean is64bits = "64".equals(System.getProperty("sun.arch.data.model"));

            String nativePath = "native";

            File libPath = new File(nativePath);
            libPath = new File(libPath, os.name().toLowerCase());
            libPath = new File(libPath, is64bits ? "lib64" : "lib");

            System.out.println(libPath.getAbsolutePath());
            if (libPath.exists()) {
                /*
                 * http://blog.cedarsoft.com/2010/11/setting-java-library-path-
                 * programmatically
                 */
                try {
                    System.setProperty("java.library.path", libPath.getAbsolutePath());
                    Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
                    fieldSysPath.setAccessible(true);
                    fieldSysPath.set(null, null);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to force the reload of system paths property.", e);
                }
            } else {
                throw new RuntimeException("Unsupported architecture: " + (is64bits ? "64" : "32") + " bits");
            }
        } else {
            throw new RuntimeException("Unsupported operating system: " + osName);
        }
    }
}
