package dev.beecube31.darktitleuni;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;

public final class Bootstrap {
    public static final String MOD_ID = "darktitlebar";

    static final String CORE_CLASS = "dev.beecube31.darktitleuni.core.DarkTitleBar";
    static final String CORE_PREFIX = "dev.beecube31.darktitleuni.core.";
    /** Stripped-down JNA 5.14.0: only classes + jnidispatch for win32-x86 / x86-64 / aarch64. */
    static final String BUNDLED_JNA = "META-INF/darktitlebar/jna-5.14.0-win.jar.bin";

    private static boolean started;

    private Bootstrap() {}

    public static synchronized void start(String loader) {
        if (started) return;
        started = true;

        try {
            if (!System.getProperty("os.name", "").startsWith("Windows")) return;
            ClassLoader own = Bootstrap.class.getClassLoader();
            ClassLoader target = own;
            if (!hasJna(own)) {
                target = isolatedLoader(own, BUNDLED_JNA);
                loader = loader + ", bundled JNA";
            }
            Class.forName(CORE_CLASS, true, target).getMethod("start", String.class).invoke(null, loader);
        } catch (Throwable t) {
            System.out.println("[DarkTitleBar] disabled: " + t);
        }
    }

    static boolean hasJna(ClassLoader cl) {
        try {
            Class.forName("com.sun.jna.Function", false, cl);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    static ClassLoader isolatedLoader(ClassLoader own, String resource) throws Exception {
        File jar = extract(own, resource);
        return new CoreLoader(new URL[]{jar.toURI().toURL()}, ClassLoader.getSystemClassLoader().getParent(), own);
    }

    private static File extract(ClassLoader own, String resource) throws Exception {
        byte[] data = read(own.getResourceAsStream(resource), resource);
        File dir = new File(System.getProperty("java.io.tmpdir"), MOD_ID);
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("cannot create " + dir);
        String name = resource.substring(resource.lastIndexOf('/') + 1).replace(".bin", "");
        File target = new File(dir, name);
        if (target.isFile() && target.length() == data.length) return target;

        File tmp = File.createTempFile("jna", ".tmp", dir);
        try (OutputStream out = Files.newOutputStream(tmp.toPath())) {
            out.write(data);
        }

        if (target.isFile()) target.delete();
        if (tmp.renameTo(target)) return target;
        if (target.isFile() && target.length() == data.length) { tmp.delete(); return target; }
        tmp.deleteOnExit();
        return tmp;
    }

    static byte[] read(InputStream in, String what) throws Exception {
        if (in == null) throw new IllegalStateException("resource not found: " + what);

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            return bos.toByteArray();
        } finally {
            in.close();
        }
    }

    static final class CoreLoader extends URLClassLoader {
        private final ClassLoader source;

        CoreLoader(URL[] urls, ClassLoader parent, ClassLoader source) {
            super(urls, parent);
            this.source = source;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (name.startsWith(CORE_PREFIX)) {
                try {
                    byte[] b = read(source.getResourceAsStream(name.replace('.', '/') + ".class"), name);
                    return defineClass(name, b, 0, b.length);
                } catch (ClassNotFoundException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
            return super.findClass(name);
        }
    }
}
