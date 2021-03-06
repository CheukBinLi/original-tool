package com.github.cheukbinli.original.common.util.scan;

import com.github.cheukbinli.original.common.util.conver.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

/***
 * scanPath="mapper.*query$xml*"
 * <p>
 * 完整路径*通配 $=.
 * </p>
 * <p>
 * 注意一定要以目录第一节点为起始
 * <p>
 * ----------例:org.spring.fw.util.cc.x.class
 * <p>
 * ----------org.*$class 或者 org.*cc.* 或者 org.*cc.x$class
 * </p>
 */
public class ScanSimple extends AbstractScan {

	private final Logger LOG = LoggerFactory.getLogger(ScanSimple.class);

	@Override
	protected Logger LOG() {
		return LOG;
	}

	public final Map<String, Set<String>> doScan(String path) throws IOException, InterruptedException, ExecutionException {
		return doScan(null, path);
	}

	public final Map<String, Set<String>> doScan(ClassLoader classLoader, String path) throws IOException, InterruptedException, ExecutionException {
		if (LOG.isDebugEnabled())
			LOG.debug("scan start...");
		Map<String, Set<String>> result = new HashMap<String, Set<String>>();
		if (null == path)
			return result;
		String[] originalPaths = path.split(",");
		path = path.replace(".", "/");
		path = path.replace(File.separator, "/").replace("$", ".");
		String[] paths = null;
		paths = path.split(",");
		String[] fullPaths = paths;
		// 后期换并发模式
		for (int i = 0, len = paths.length; i < len; i++) {
			Enumeration<URL> urls = (null == classLoader ? Thread.currentThread().getContextClassLoader() : classLoader).getResources(paths[i].contains("*") ? paths[i].split("/")[0] : paths[i]);
			Set<URL> scanResult = new LinkedHashSet<URL>();
			while (urls.hasMoreElements()) {
				scanResult.add(urls.nextElement());
			}
			result.put(originalPaths[i], classMatchFilter(fullPaths[i], scanResult));
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("scan finish. \n" + result.toString());
		}
		return result;
	}

	protected final Set<String> classMatchFilter(final String path, Set<URL> paths) throws InterruptedException, ExecutionException {
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		final String pathPattern = "^(/.*/|.*/)?" + path.replace("*", "(.*)?").replace("(.*)?(.*)?", "(.*)?").replace("(.*)?/(.*)?", "(/.*|.*/)?").replace("/.*/.*", "/.*") + "(/.*)?$";
		// packageName
		// final int startIndex = (new
		// File(Thread.currentThread().getContextClassLoader().getResource("").getPath())).getPath().replace(File.separator,
		// "/").length() + 1;
		Set<URL> jarClassPaths = new HashSet<URL>();
		Set<URL> fileClassPaths = new HashSet<URL>();
		Set<String> result = new HashSet<String>();
		List<Future<Set<String>>> futures = new ArrayList<Future<Set<String>>>();
		final CountDownLatch countDownLatch = new CountDownLatch(2);
		Iterator<URL> urls = paths.iterator();
		URL u;
		while (urls.hasNext()) {
			u = urls.next();
			if ("jar".equals(u.getProtocol()))
				jarClassPaths.add(u);
			else
				fileClassPaths.add(u);
		}
		// 过滤
		futures.add(executorService.submit(new ScanSimple.FileFilter(jarClassPaths, pathPattern, null, countDownLatch) {
			@Override
			public Set<String> doFilter(Set<URL> url, String pathPattern, String startIndex) throws IOException {
				return jarTypeFilter(pathPattern, url);
			}
		}));
		futures.add(executorService.submit(new ScanSimple.FileFilter(fileClassPaths, pathPattern, path.substring(0, path.contains("*") ? path.indexOf("*") : path.length()), countDownLatch) {
			@Override
			public Set<String> doFilter(Set<URL> url, String pathPattern, String startIndex) {
				Iterator<URL> it = url.iterator();
				Set<String> result = new HashSet<String>();
				while (it.hasNext())
					result.addAll(fileTypeFilter(toFilePath(it.next()), pathPattern, startIndex));
				return result;
			}
		}));
		if (jarClassPaths.isEmpty())
			countDownLatch.countDown();
		if (fileClassPaths.isEmpty())
			countDownLatch.countDown();

		countDownLatch.await();

		result.addAll(futures.get(0).get());
		result.addAll(futures.get(1).get());
		try {
			return result;
		} finally {
			executorService.shutdown();
		}
	}

	@SuppressWarnings("resource")
	protected final Set<String> jarTypeFilter(String pathPattern, Set<URL> urls) throws IOException {
		Set<String> result = new HashSet<String>();
		Iterator<URL> it = urls.iterator();
		URL u;
		String jarPath;
		String[] jarPaths;
		String name;
		while (it.hasNext()) {
			u = it.next();
			if (StringUtil.concatCount(jarPath = u.getPath(), "jar!") == 2) {
				jarPaths = jarPath.split("!");
//				JarFile jarFile = new JarFile(new File(jarPaths[0].replaceAll("file:/", "").replaceAll("file:", "")));
				JarFile jarFile = new JarFile(toFilePath(u));
				JarInputStream jarInputStream = new JarInputStream(jarFile.getInputStream(jarFile.getJarEntry(jarPaths[1].substring(1))));
				ZipEntry zipEntry = null;
				while (null != (zipEntry = jarInputStream.getNextEntry())) {
					if (!zipEntry.isDirectory() && (name = zipEntry.getName()).matches(pathPattern)) {
						result.add(name);
					}
				}
			} else {
				JarFile jarFile = new JarFile(toFilePath(u));
				Enumeration<JarEntry> jars = jarFile.entries();
				while (jars.hasMoreElements()) {
					JarEntry jarEntry = jars.nextElement();
					if (null == jarEntry || jarEntry.isDirectory()) {
						continue;
					}
					// if ((name =
					// jarEntry.getName()).toLowerCase().endsWith("jar")) {
					// jarInJar(result, pathPattern,
					// jarFile.getInputStream(jarEntry));
					// }
					if ((name = jarEntry.getName()).matches(pathPattern)) {
						// result.add(jarEntry.getName().replace("/", "."));
						result.add(name);
					}
				}
			}
		}
		return result;
	}

	@SuppressWarnings("resource")
	@Deprecated
	protected final void jarInJar(final Set<String> result, String pathPattern, InputStream in) throws IOException {
		ZipEntry jarEntry = null;
		String name;
		JarInputStream jarInputStream = new JarInputStream(in);
		while (null != (jarEntry = jarInputStream.getNextEntry())) {
			if (jarEntry.isDirectory())
				continue;
			if ((name = jarEntry.getName()).toLowerCase().endsWith("jar")) {
				// 末实现
				continue;
			}
			if (name.matches(pathPattern)) {
				result.add(jarEntry.getName());
			}
		}

	}

	protected final Set<String> fileTypeFilter(File file, String pathPattern, String startIndex) {
		// Map<String, String> result = new WeakHashMap<String, String>();
		Set<String> result = new HashSet<String>();
		try {
			String filePath;
			if (file.isFile()) {
				if ((filePath = file.getPath().replace(File.separator, "/")).matches(pathPattern)) {
					// 文件添加返回
					result.add(filePath.substring(filePath.indexOf(startIndex)));
				}
				return result;
			} else if (file.isDirectory()) {
				File[] files = file.listFiles();
				for (File f : files) {
					// 目录递归
					result.addAll(fileTypeFilter(f, pathPattern, startIndex));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	abstract class FileFilter implements Callable<Set<String>> {

		private String pathPattern;
		private Set<URL> urls;
		private String startIndex;
		private final CountDownLatch countDownLatch;

		public FileFilter(Set<URL> urls, String pathPattern, final CountDownLatch countDownLatch) {
			super();
			this.pathPattern = pathPattern;
			this.urls = urls;
			this.countDownLatch = countDownLatch;
		}

		public FileFilter(Set<URL> urls, String pathPattern, String startIndex, final CountDownLatch countDownLatch) {
			super();
			this.pathPattern = pathPattern;
			this.startIndex = startIndex;
			this.urls = urls;
			this.countDownLatch = countDownLatch;
		}

		public abstract Set<String> doFilter(Set<URL> url, String pathPattern, String startIndex) throws Exception;

		public Set<String> call() throws Exception {
			try {
				Set<String> result = doFilter(urls, pathPattern, startIndex);
				return result;
			} catch (Exception e) {
				throw e;
			} finally {
				if (null != countDownLatch) {
					countDownLatch.countDown();
				}
			}
		}

	}
	
	File toFilePath(URL path) {
		try {
			String filePath = URLDecoder.decode(path.getPath(), "UTF-8");
			int start = filePath.startsWith("file:") ? 5 : 0;
			int end = filePath.contains("!") ? filePath.indexOf("!") : filePath.length();
			filePath = filePath.substring(start, end);
			File result = new File(filePath);
			if (result.isFile() || result.isDirectory())
				return result;
			return new File(filePath.startsWith("/") ? filePath.substring(1) : filePath);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws Throwable {

		// Map<String, Set<String>> result = new
		// ScanSimple().doScan("META-INF.maven.*xml,com.cheuks.*");
		AbstractScan scan = new ScanSimple();
		scan.setScanPath("META-INF.maven.*xml,com.cheuks.*,mapper.*query$xml*,org/apache/*/spi/**Root*$class,com.cheuks.*,org.springframework.orm.*");
		Set<String> result = scan.getResource("org/apache/*/spi/*/*Root*$class");
		Set<String> result2 = scan.getResource("com.cheuks.*");
		Set<String> result3 = scan.getResource("org.springframework.orm.*");
		// Set<String> result =
		// scan.getResource("org.apache.*.spi.*Root*$class");
		// Set<String> result = scan.getResource("mapper.*query$xml*");
		Set<String> result6 = scan.getResource("META-INF.maven.*xml");
		// Map<String, Set<String>> result1 = scan.resource;
		System.out.println(result);
		System.out.println(result2);
		System.out.println(result3);
		System.out.println(result6);
		// System.out.println(result1.toString());
	}

}
