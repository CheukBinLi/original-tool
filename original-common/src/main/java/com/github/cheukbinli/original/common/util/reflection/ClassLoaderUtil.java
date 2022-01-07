package com.github.cheukbinli.original.common.util.reflection;

import com.github.cheukbinli.original.common.cache.CacheException;
import com.github.cheukbinli.original.common.util.conver.StringUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

@SuppressWarnings("valuegoeshere")
public class ClassLoaderUtil {

    // @SuppressWarnings("static-access")
    // public static ClassLoader createClassLoader(ClassLoader
    // parentClassLoader, URL... jars) {
    // URLClassLoader classLoader = new URLClassLoader(jars, null ==
    // parentClassLoader ?
    // ClassLoaderUtil.class.getClassLoader().getSystemClassLoader() :
    // parentClassLoader);
    // return classLoader;
    // }

    //    static Method getJarFile;
    static Field ucpField;
    static Method getLoaderMethod, ensureOpenMethod, getJarFileMethod;

//    static {
//        try {
//            getJarFile = DefaultURLJarFile.class.getDeclaredMethod("getJarFile", URL.class);
//            getJarFile.setAccessible(true);
//        } catch (NoSuchMethodException e) {
//            throw new RuntimeException(e);
//        }
//    }

    public static class CustomClassLoader<T> extends URLClassLoader {

        private T resource;

        public T getResource() {
            return resource;
        }

        public void setResource(T resource) {
            this.resource = resource;
        }

        public CustomClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        public CustomClassLoader(URL[] urls) {
            super(urls);
        }

        public CustomClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
            super(urls, parent, factory);
        }
    }

    protected static ClassLoader createParentLoader() throws NoSuchMethodException, SecurityException, MalformedURLException {

        SimpleClassLoader classLoader = new SimpleClassLoader();
        String classPath = System.getProperty("java.class.path");
        if (StringUtil.isBlank(classPath)) {
            throw new NoSuchMethodException("cant't found \"java.class.path\" system property");
        }
        String separator = classPath.contains(";") ? ";" : ":";
        String[] classPaths = classPath.split(separator);
        for (String item : classPaths) {
            classLoader.addURL(new File(item));
        }
        return classLoader;
    }

    private static final Set<String> PROTOCOLS = new HashSet<>(Arrays.asList("http", "https", "file"));

    public static ClassLoader createClassLoaderByFile(ClassLoader parentClassLoader, String jarFilePatch) throws Exception {

        final Set<URL> jars = new HashSet<>();

        Files.walkFileTree(Paths.get(jarFilePatch), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                jars.add(file.toFile().toURL());
                return super.visitFile(file, attrs);
            }
        });
        return createClassLoader(parentClassLoader, jars.toArray(new URL[0]));
    }

    /***
     *
     * @param parentClassLoader  为null时，默认依赖 ${java.class.path}路径下所有包
     * @param jars
     * @return
     * @throws Exception
     */
    public static ClassLoader createClassLoader(ClassLoader parentClassLoader, URL... jars) throws Exception {
        if (null != jars)
            for (URL jar : jars) {
                if (!PROTOCOLS.contains(jar.getProtocol().toLowerCase())) {
                    throw new Exception("cant't found jar protocol(http/https/file)");
                }
            }
        if (null == parentClassLoader) {
            parentClassLoader = createParentLoader();
        }
        return new CustomClassLoader(jars, parentClassLoader);
    }

    @SuppressWarnings({"static-access"})
    public static ClassLoader createClassLoader(ClassLoader parentClassLoader, String... jars) throws Exception {
        List<URL> urls = new LinkedList<>();
        boolean support;
        if (null != jars)
            for (String jar : jars) {
                support = false;
                for (String protocol : PROTOCOLS) {
                    if (jar.startsWith(protocol.toLowerCase())) {
                        support = true;
                        break;
                    }
                }
                if (!support)
                    throw new Exception("cant't found jar protocol(http/https/file)");
                urls.add(new URL(jar));
            }
        if (null == parentClassLoader) {
            parentClassLoader = ClassLoaderUtil.class.getClassLoader().getSystemClassLoader();
        }
        return new CustomClassLoader(urls.toArray(new URL[0]), parentClassLoader);
    }

//    static DefaultURLJarFile createURLJarFile(URL url) throws IllegalAccessException, InvocationTargetException, InstantiationException {
//        return (DefaultURLJarFile) getJarFile.invoke(null, url);
//    }

    @SuppressWarnings({"static-access"})
    public static ClassLoaderInfo createClassLoaderInfo(ClassLoader parentClassLoader, URL... jars) throws Exception {

        ClassLoaderInfo result = new ClassLoaderInfo();
        Map<String, ClassLoaderInfo.JarClassInfo> jarClassInfo = null;
        Map<String, ClassLoaderInfo.JarEntry> classes;
        Map<String, List<ClassLoaderInfo.JarEntry>> files;

        if (null != jars) {
            jarClassInfo = new HashMap<>((int) (jars.length + jars.length * 0.3));
            ZipEntry zipEntry;
//            JarInputStream jarInputStream;
            String path, name, simpleName;
            int length;
            byte[] buffer;
            InputStream inputStream;
            for (URL jar : jars) {
                classes = new HashMap<>();
                files = new HashMap<>();
//                jarInputStream = new JarInputStream(jar.openStream());

//                DefaultURLJarFile urlJarFile = createURLJarFile(jar);
                JarURLConnection jarURLConnection = null;
                try {
                    jarURLConnection = (JarURLConnection) jar.openConnection();
                } catch (Throwable e) {
                    e.printStackTrace();
                    continue;
                }
                if (null == jarURLConnection) {
                    continue;
                }
                JarFile urlJarFile = jarURLConnection.getJarFile();
                if (null == urlJarFile) {
                    continue;
                }

                Enumeration<JarEntry> enumeration = urlJarFile.entries();
                while (enumeration.hasMoreElements()) {
                    if (null == (zipEntry = enumeration.nextElement()) || zipEntry.isDirectory()) {
                        continue;
                    }
                    if (zipEntry.getName().endsWith(".class")) {
                        name = zipEntry.getName().replace("/", ".");
                        name = name.substring(0, name.length() - 6);
                        classes.put(name, new ClassLoaderInfo
                                .JarEntry()
                                .setName(name)
                                .setSimpleName(name.substring(name.lastIndexOf(".") + 1))
                                .setType("class"));
                    } else {
                        name = zipEntry.getName();
                        name = name.startsWith("/") ? name : "/" + name;
                        simpleName = name.substring(length = name.lastIndexOf("/") + 1);
                        path = name.substring(0, length);

                        List<ClassLoaderInfo.JarEntry> jarEntries = files.get(path);

                        if (null == jarEntries) {
                            files.put(path, jarEntries = new ArrayList<>());
                        }

                        buffer = new byte[Long.valueOf(zipEntry.getSize()).intValue()];
                        (inputStream = urlJarFile.getInputStream(zipEntry)).read(buffer);
                        inputStream.close();

                        jarEntries.add(new ClassLoaderInfo.JarEntry()
                                .setName(simpleName)
                                .setPath(path)
                                .setContent(buffer)
                                .setSimpleName(simpleName)
                                .setType(simpleName.substring(simpleName.lastIndexOf(".") + 1)));
                    }
                }
                if (!PROTOCOLS.contains(jar.getProtocol().toLowerCase())) {
                    throw new Exception("(" + jar.getPath() + ")only support protocol(http/https/file)");
                }
//                jarInputStream.close();
                urlJarFile.close();
                jarClassInfo.put(jar.getPath(), new ClassLoaderInfo.JarClassInfo().setClasses(classes).setFiles(files));
            }
        }
        if (null == parentClassLoader) {
            parentClassLoader = createParentLoader();
        }

        result
                .setClassLoader(new CustomClassLoader(jars, parentClassLoader)).setJarClassInfo(jarClassInfo);
        return result;
    }

    public static void destroy(ClassLoader classLoader) {
        Thread thread = Thread.currentThread();
        synchronized (thread) {
            if (null == classLoader)
                return;
            ClassLoader parent = Thread.currentThread().getContextClassLoader();
            thread.setContextClassLoader(classLoader);

            ThreadGroup currentGroup = thread.getThreadGroup();
            while (currentGroup.getParent() != null) {
                currentGroup = currentGroup.getParent();
            }
            int count = currentGroup.activeCount();
            Thread[] threads = new Thread[count];
            currentGroup.enumerate(threads);
            for (Thread item : threads) {
                try {
                    if (null == item.getContextClassLoader() || !item.getContextClassLoader().equals(classLoader))
                        continue;
                    item.interrupt();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            thread.setContextClassLoader(parent);
        }
        releaseLoader((URLClassLoader) classLoader);
//        sun.misc.ClassLoaderUtil.releaseLoader((URLClassLoader) classLoader);
    }

    public static class ClassLoaderInfo implements Serializable {
        private static final long serialVersionUID = -5540285480331355776L;

        private ClassLoader classLoader;
        private Map<String, JarClassInfo> jarClassInfo;

        public static class JarClassInfo implements Serializable {
            private static final long serialVersionUID = -1509900969440284636L;

            private String name;
            private Map<String, List<JarEntry>> files;
            private Map<String, JarEntry> classes;

            public String getName() {
                return name;
            }

            public JarClassInfo setName(String name) {
                this.name = name;
                return this;
            }

            public Map<String, List<JarEntry>> getFiles() {
                return files;
            }

            public JarClassInfo setFiles(Map<String, List<JarEntry>> files) {
                this.files = files;
                return this;
            }

            public Map<String, JarEntry> getClasses() {
                return classes;
            }

            public JarClassInfo setClasses(Map<String, JarEntry> classes) {
                this.classes = classes;
                return this;
            }
        }

        public static class JarEntry implements Serializable {
            private static final long serialVersionUID = -4145184904978633147L;

            private String path;
            private String type;
            private String name;
            private String simpleName;
            private byte[] content;

            public String getPath() {
                return path;
            }

            public JarEntry setPath(String path) {
                this.path = path;
                return this;
            }

            public String getType() {
                return type;
            }

            public JarEntry setType(String type) {
                this.type = type;
                return this;
            }

            public String getName() {
                return name;
            }

            public JarEntry setName(String name) {
                this.name = name;
                return this;
            }

            public String getSimpleName() {
                return simpleName;
            }

            public JarEntry setSimpleName(String simpleName) {
                this.simpleName = simpleName;
                return this;
            }

            public byte[] getContent() {
                return content;
            }

            public JarEntry setContent(byte[] content) {
                this.content = content;
                return this;
            }
        }

        public ClassLoader getClassLoader() {
            return classLoader;
        }

        public ClassLoaderInfo setClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public Map<String, JarClassInfo> getJarClassInfo() {
            return jarClassInfo;
        }

        public ClassLoaderInfo setJarClassInfo(Map<String, JarClassInfo> jarClassInfo) {
            this.jarClassInfo = jarClassInfo;
            return this;
        }
    }

    static Object getUcpObj(URLClassLoader classLoader) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException {
        Object ucpObj = null;
        if (null == ucpField) {
            ucpField = URLClassLoader.class.getDeclaredField("ucp");
            ucpField.setAccessible(true);
        }
        ucpObj = ucpField.get(classLoader);
        if (null == getLoaderMethod) {
            getLoaderMethod = ucpObj.getClass().getDeclaredMethod("getLoader", int.class);
            getLoaderMethod.setAccessible(true);
        }
        return ucpObj;
    }

    static synchronized void releaseLoader(URLClassLoader classLoader) {
        try {
            if (!(classLoader instanceof URLClassLoader)) {
                return;
            }
            // 查找URLClassLoader中的ucp
            Object ucpObj = getUcpObj(classLoader);
            URL[] list = classLoader.getURLs();

            for (int i = 0; i < list.length; i++) {
                // 获得ucp内部的jarLoader
                Object jarLoader = getLoaderMethod.invoke(ucpObj, i);
                if (null == jarLoader) {
                    continue;
                }
                String clsName = jarLoader.getClass().getName();
                if (clsName.indexOf("JarLoader") != -1) {
                    if (null == ensureOpenMethod) {
                        ensureOpenMethod = jarLoader.getClass().getDeclaredMethod("ensureOpen");
                        ensureOpenMethod.setAccessible(true);
                    }
                    ensureOpenMethod.invoke(jarLoader);
                    if (null == getJarFileMethod) {
                        getJarFileMethod = jarLoader.getClass().getDeclaredMethod("getJarFile");
                        getJarFileMethod.setAccessible(true);
                    }
                    JarFile jf = (JarFile) getJarFileMethod.invoke(jarLoader);
                    // 释放jarLoader中的jar文件
                    jf.close();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({"unused", "static-access", "unchecked", "rawtypes", "deprecation"})
    public static void main(String[] args) throws CacheException, Exception {

        ClassLoaderInfo classLoaderInfo = ClassLoaderUtil.createClassLoaderInfo(null, new URL("https://repo1.maven.org/maven2/org/slf4j/jul-to-slf4j/1.7.31/jul-to-slf4j-1.7.31.jar"));
        Object o = classLoaderInfo.getClassLoader().getResource("META-INF");
        ClassLoaderUtil.destroy(classLoaderInfo.getClassLoader());
        System.out.printf(URLDecoder.decode("https%3A%2F%2Fai-test.bgyfws.com%3A55871%2Failogic"));
        ClassLoader x = new ClassLoader() {
        };
        System.err.println(System.getProperty("java.class.path"));

        File f1 = new File("D:\\repository\\maven\\com\\github\\cheukbinli\\original-cache\\1.0.0.3.6-RELEASE\\original-cache-1.0.0.3.6-RELEASE.jar");
//        File f1 = new File("D:/repository/maven/com/cheuks/bin/original-cache/0.0.1-SNAPSHOT/original-cache-0.0.1-SNAPSHOT.jar");
        File f2 = new File("D:/Desktop/remote-test.jar");
        System.err.println(f1.toURI().toURL().toString());
        // URLClassLoader cl = new URLClassLoader(new URL[]{f1.toURI().toURL()},
        // ClassLoaderUtil.class.getClassLoader().getSystemClassLoader());
        // Class<?> xxxx =
        // cl.loadClass("com.github.cheukbinli.original.cache.DefaultCacheSerialize");
        // CacheSerialize cs = (CacheSerialize)
        // cl.loadClass("com.github.cheukbinli.original.cache.DefaultCacheSerialize").newInstance();
        // String a = "abcde";
        // byte[] data_template = cs.encode(a);
        // System.err.println(cs.decodeT(data_template, String.class));

       /* ClassLoader cl = createClassLoader(null, f1.toURI().toURL(), f2.toURL());
        // Thread thread=Thread.currentThread();
        // thread.setContextClassLoader(cl);
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    for (; ; ) {
                        Thread.sleep(1000);
                        System.out.println("xxxxxxxxxxxx");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
        // Thread.sleep(5000);
        // destroy(cl);
        // Thread.sleep(5000);
        // destroy(cl);
        System.err.println(ClassLoaderUtil.class.getClass().getClassLoader().getSystemClassLoader());
        Class test = cl.loadClass("a.a.a.threadxx");
        Object instace = test.newInstance();
        Method method = test.getDeclaredMethod("runxxx");
        method.invoke(instace);
        Thread.sleep(5000);
        destroy(cl);
        // Thread.sleep(5000);
        // Thread.sleep(5000);
        // destroy(cl);
        Thread.sleep(5000);
        CountDownLatch c = new CountDownLatch(1);
        c.await();*/

        File f11 = new File("D:/SYSTEM/Desktop/1.jar");
        File f22 = new File("D:/SYSTEM/Desktop/2.jar");
        System.err.println(f1.toURI().toURL().toString());

//        ClassLoader cl1 = ClassLoaderUtil.createClassLoader(null, f11.toURI().toURL());
//        ClassLoader cl2 = ClassLoaderUtil.createClassLoader(null, f22.toURI().toURL());
        ClassLoader cl1 = ClassLoaderUtil.createClassLoader(null, f1.toURI().toURL());

        Class c1 = cl1.loadClass("com.github.cheukbinli.original.cache.DefaultCacheSerialize");
//        Class c1 = cl1.loadClass("com.github.cheukbinli.original.cache.A");
//        Class c2 = cl2.loadClass("com.github.cheukbinli.original.cache.A");
        c1.newInstance();
//        c2.newInstance();

        releaseLoader((URLClassLoader) cl1);

        System.out.println(1111);

    }

}
