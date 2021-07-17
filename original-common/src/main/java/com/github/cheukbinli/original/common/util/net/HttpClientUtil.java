package com.github.cheukbinli.original.common.util.net;

import com.github.cheukbinli.original.common.util.conver.CollectionUtil;
import com.github.cheukbinli.original.common.util.conver.StringUtil;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class HttpClientUtil {

	public static enum RequestMethod {
		POST, DELETE, PUT, GET, HEAD, Options
	}

	protected HttpClientUtil() {
	}

	private static HttpClientUtil INSTANCE;

	public static final HttpClientUtil newInstance() {
		if (null == INSTANCE) {
			synchronized (HttpClientUtil.class) {
				if (null == INSTANCE) {
					INSTANCE = new HttpClientUtil();
				}
			}
		}
		return INSTANCE;
	}

	/***
	 * 文件上传到微信服务器
	 *
	 * @param url
	 * @param fileName
	 * @param inputStream
	 * @throws Exception
	 */
	public ByteArrayOutputStream sendFile(String url, String fileName, InputStream inputStream) throws Exception {
		return sendFile(url, fileName, inputStream, null);
	}

	public ByteArrayOutputStream sendFile(String url, String fileName, InputStream inputStream, Map<String, String> header) throws Exception {
		return fromData(url, new Body().append(new Body.BodyItem.FileBodyItem("file", fileName, inputStream)), header);
	}

	public ByteArrayOutputStream fromData(String url, Body body, Map<String, String> header) throws Exception {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		URL urlObj = new URL(url);
		boolean isHttps = url.toLowerCase().contains("https:");
		HttpURLConnection con = null;
		InputStream in;
		try {
			if (isHttps)
				con = (HttpsURLConnection) urlObj.openConnection();
			else
				con = (HttpURLConnection) urlObj.openConnection();
			con.setRequestMethod("POST");
			con.setDoInput(true);
			con.setDoOutput(true);
			con.setUseCaches(false);
			con.setRequestProperty("Connection", "Keep-Alive");
			con.setRequestProperty("Charset", "UTF-8");
			if (null != header) {
				for (Entry<String, String> item : header.entrySet()) {
					con.setRequestProperty(item.getKey(), item.getValue());
				}
			}
			String BOUNDARY = "----------" + System.currentTimeMillis();
			con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);

			byte[] separate = ("\r\n--" + BOUNDARY + "--\r\n").getBytes("utf-8");
			OutputStream out = new DataOutputStream(con.getOutputStream());
			for (Body.BodyItem item : body.getBodyItems()) {
				out.write(separate);
				item.writeBody(out);
				out.write(separate);
			}

			out.flush();
			out.close();

			int bytes = 0;
			byte[] bufferOut = new byte[1024];
			in = con.getInputStream();
			while ((bytes = in.read(bufferOut)) != -1) {
				result.write(bufferOut, 0, bytes);
			}
			in.close();
		} finally {
			if (null != con)
				con.disconnect();
		}
		return result;
	}

	/**
	 * 默认：value进行trim处理
	 * */
	public HttpResponseModel fromData(String url, Map<String, Object> params, boolean onlyRequest, boolean onlyResponseData, Map<String, String> header) throws Exception {
		return fromData(url, params, true, onlyRequest, onlyResponseData, header);
	}
	
	public HttpResponseModel fromData(String url, Map<String, Object> params, boolean isTrim, boolean onlyRequest, boolean onlyResponseData, Map<String, String> header) throws Exception {
		ByteArrayOutputStream out;
		HttpResponseModel result;
		URL urlObj = new URL(url);
		boolean isHttps = url.toLowerCase().contains("https:");
		HttpURLConnection con = null;
		InputStream in;
		try {
			if (isHttps)
				con = (HttpsURLConnection) urlObj.openConnection();
			else
				con = (HttpURLConnection) urlObj.openConnection();
			con.setRequestMethod("POST");
			con.setDoInput(true);
			con.setDoOutput(true);
			con.setUseCaches(false);
			con.setRequestProperty("Connection", "Keep-Alive");
			con.setRequestProperty("Charset", "UTF-8");
			if (null != header) {
				for (Entry<String, String> item : header.entrySet()) {
					con.setRequestProperty(item.getKey(), item.getValue());
				}
			}
			String BOUNDARY = "" + System.currentTimeMillis();
			con.setRequestProperty("Content-Type", "multipart/form-data; boundary=----" + BOUNDARY);
			con.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.8");
			con.setRequestProperty("Accept", "*/*");
			con.setRequestProperty("Range", "bytes=" + "");
			StringBuilder sb = new StringBuilder();

			if (!CollectionUtil.isEmpty(params)) {
				params.forEach((k, v) -> {
					sb.append("------").append(BOUNDARY).append("\r\n");
					sb.append("Content-Disposition: form-data; name=\"");
					sb.append(k);
					sb.append("\"\r\n\r\n");
					sb.append(null == v ? null : isTrim ? v.toString().trim() : v.toString());
					sb.append("\r\n");
				});
			}
			sb.append("------").append(BOUNDARY).append("--\r\n");
			OutputStream outWriter = new DataOutputStream(con.getOutputStream());
			outWriter.write(sb.toString().getBytes("utf-8"));
			outWriter.flush();
			outWriter.close();

			result = new HttpResponseModel(con.getResponseCode(), onlyResponseData ? null : con.getHeaderFields(), out = new ByteArrayOutputStream());
			if (onlyRequest)
				return result;
			int bytes = 0;
			byte[] bufferOut = new byte[1024];
			in = con.getInputStream();
			while ((bytes = in.read(bufferOut)) != -1) {
				out.write(bufferOut, 0, bytes);
			}
			in.close();
		} finally {
			if (null != con)
				con.disconnect();
		}
		return result;
	}

	/***
	 * get请求
	 * 
	 * @param urlPath
	 * @param timeOut
	 * @param onlyRequest
	 * @return
	 * @throws IOException
	 */
	public HttpResponseModel GET(String urlPath, int timeOut, boolean onlyRequest, boolean onlyResponseData) throws IOException {
		return GET(urlPath, timeOut, onlyRequest, onlyResponseData, null);
	}

	public HttpResponseModel GET(String urlPath, int timeOut, boolean onlyRequest, boolean onlyResponseData, Map<String, String> header) throws IOException {
		return GET(urlPath,timeOut,onlyRequest,onlyResponseData,header,"");
	}
	public HttpResponseModel GET(String urlPath, int timeOut, boolean onlyRequest, boolean onlyResponseData, Map<String, String> header, Map<Object, Object> params) throws IOException {
		return GET(urlPath,timeOut,onlyRequest,onlyResponseData,header, StringUtil.assemble("=", params, "&"));
	}
	public HttpResponseModel GET(String urlPath, int timeOut, boolean onlyRequest, boolean onlyResponseData, Map<String, String> header,String parameterStr) throws IOException {
		HttpURLConnection con = null;
		URL url = null;
		InputStream in = null;
		ByteArrayOutputStream out;
		HttpResponseModel result;
		boolean isHttps = urlPath.toLowerCase().contains("https:");
		try {
			url = new URL(urlPath);
			if (isHttps)
				con = (HttpsURLConnection) url.openConnection();
			else
				con = (HttpURLConnection) url.openConnection();
			con.setUseCaches(false);
			con.setDoInput(true);
			con.setDoOutput(true);
			con.setRequestMethod("GET");
			con.setReadTimeout(timeOut);
			if (null != header) {
				for (Entry<String, String> item : header.entrySet()) {
					con.setRequestProperty(item.getKey(), item.getValue());
				}
			}
			if (StringUtil.isBlank(parameterStr)) {
				con.connect();
			} else {
				OutputStream outWriter = new DataOutputStream(con.getOutputStream());
				outWriter.write(parameterStr.getBytes("utf-8"));
				outWriter.flush();
				outWriter.close();
			}
			result = new HttpResponseModel(con.getResponseCode(), onlyResponseData ? null : con.getHeaderFields(), out = new ByteArrayOutputStream());
			if (onlyRequest)
				return result;
			in = con.getInputStream();
			byte[] buffer = new byte[512];
			int length;
			while ((length = in.read(buffer)) != -1) {
				out.write(buffer, 0, length);
			}
			in.close();
		} finally {
			if (null != con)
				con.disconnect();
		}
		return result;
	}

	/***
	 * POST 请求
	 * 
	 * @param urlPath
	 * @param parameterStr
	 * @param timeOut
	 * @param onlyRequest
	 * @return
	 * @throws IOException
	 */
	public HttpResponseModel POST(String urlPath, String parameterStr, int timeOut, boolean onlyRequest, boolean onlyResponseData, Map<String, String> header) throws IOException {
		return requestForMethodType(RequestMethod.POST, urlPath, parameterStr, timeOut, onlyRequest, onlyResponseData, header);
	}

	public HttpResponseModel PUT(String urlPath, String parameterStr, int timeOut, boolean onlyRequest, boolean onlyResponseData, Map<String, String> header) throws IOException {
		return requestForMethodType(RequestMethod.PUT, urlPath, parameterStr, timeOut, onlyRequest, onlyResponseData, header);
	}

	public HttpResponseModel DELETE(String urlPath, String parameterStr, int timeOut, boolean onlyRequest, boolean onlyResponseData, Map<String, String> header) throws IOException {
		return requestForMethodType(RequestMethod.DELETE, urlPath, parameterStr, timeOut, onlyRequest, onlyResponseData, header);
	}

	public HttpResponseModel HEAD(String urlPath, String parameterStr, int timeOut, boolean onlyRequest, boolean onlyResponseData, Map<String, String> header) throws IOException {
		return requestForMethodType(RequestMethod.HEAD, urlPath, parameterStr, timeOut, onlyRequest, onlyResponseData, header);
	}

	public HttpResponseModel Options(String urlPath, String parameterStr, int timeOut, boolean onlyRequest, boolean onlyResponseData, Map<String, String> header) throws IOException {
		return requestForMethodType(RequestMethod.Options, urlPath, parameterStr, timeOut, onlyRequest, onlyResponseData, header);
	}

	public HttpResponseModel requestForMethodType(RequestMethod requestMethod, String urlPath, String parameterStr, int timeOut, boolean onlyRequest, boolean onlyResponseData, Map<String, String> header) throws IOException {
		HttpURLConnection con = null;
		URL url = null;
		InputStream in = null;
		OutputStream out = null;
		ByteArrayOutputStream data;
		HttpResponseModel result;
		boolean isHttps = urlPath.toLowerCase().contains("https:");
		try {
			url = new URL(urlPath);
			if (isHttps)
				con = (HttpsURLConnection) url.openConnection();
			else
				con = (HttpURLConnection) url.openConnection();
			con.setUseCaches(false);
			con.setDoInput(true);
			con.setDoOutput(true);
			con.setRequestMethod(requestMethod.toString());
			con.setReadTimeout(timeOut);
			if (null != header) {
				for (Entry<String, String> item : header.entrySet()) {
					con.setRequestProperty(item.getKey(), item.getValue());
				}
			}
			con.connect();
			out = con.getOutputStream();
			out.write(parameterStr.getBytes("UTF-8"));
			out.flush();
			result = new HttpResponseModel(con.getResponseCode(), onlyResponseData ? null : con.getHeaderFields(), data = new ByteArrayOutputStream());
			if (onlyRequest)
				return result;
			in = con.getInputStream();
			byte[] buffer = new byte[512];
			int length;
			while ((length = in.read(buffer)) != -1) {
				data.write(buffer, 0, length);
			}
			in.close();
		} finally {
			if (null != out) {
				out.close();
			}
			if (null != in) {
				in.close();
			}
			con.disconnect();
		}
		return result;
	}

	public static void ignoreCerts() throws NoSuchAlgorithmException, KeyManagementException {
		TrustManager[] trustAllCerts = new TrustManager[1];
		trustAllCerts[0] = new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
			}
		};

		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, null);
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
			public boolean verify(String urlHostName, SSLSession session) {
				return true;
			}
		});
	}

	public static class Body implements Serializable {
		private volatile List<BodyItem> bodyItems = new ArrayList<>();

		public Body append(BodyItem item) {
//			if (null == this.list) {
//				synchronized (Body.class) {
//					if (null == this.list) {
//						this.list = new ArrayList<>();
//					}
//				}
//			}
			bodyItems.add(item);
			return this;
		}

		public Body append(String fieldName, String nickName, Object value, BodyItem.BodyItemType type) {
			this.append(BodyItem.BodyItemType.FILE == type ? new BodyItem.FileBodyItem(fieldName, nickName, (InputStream) value) : new BodyItem.TextBodyItem(fieldName, (String) value));
			return this;
		}

		public static abstract class BodyItem<T> implements Serializable {
			private static final long serialVersionUID = -2449963715563826850L;
			public enum BodyItemType{
				TEXT, FILE;
			}
			private final BodyItemType type;
			private String fieldName;
			private T value;

			public BodyItem(BodyItemType type, String fieldName, T value) {
				this.type = type;
				this.fieldName = fieldName;
				this.value = value;
			}

			public abstract String getHead() throws IOException;
			public abstract void writeBody(OutputStream out) throws IOException;

			public static class FileBodyItem extends BodyItem<InputStream> {
				private static final long serialVersionUID = -8503425365672381515L;

				private String fileName;

				public FileBodyItem(String fileName, String fieldName, InputStream value) {
					super(BodyItemType.FILE, fileName, value);
					this.fileName = fieldName;
				}

				@Override
				public String getHead() throws IOException {
					return "Content-Disposition: form-data;name=\"" + getFileName() + "\";filename=\"" + getFileName() + "\"\r\n" +
							"Content-Type:application/octet-stream\r\n\r\n";
				}

				@Override
				public void writeBody(OutputStream out) throws IOException {
					int bytes = 0;
					byte[] bufferOut = new byte[1024];
					while ((bytes = getValue().read(bufferOut)) != -1) {
						out.write(bufferOut, 0, bytes);
					}
					getValue().close();
				}

				public String getFileName() {
					return fileName;
				}

				public void setFileName(String fileName) {
					this.fileName = fileName;
				}
			}

			public static class TextBodyItem extends BodyItem<String> {
				private static final long serialVersionUID = 7671901585936781934L;

				public TextBodyItem(String name, String value) {
					super(BodyItemType.TEXT, name, value);
				}

				@Override
				public String getHead() throws IOException {
					return "Content-Disposition: form-data;name=\"" + getFieldName() + "\"\r\n\r\n";
				}

				@Override
				public void writeBody(OutputStream out) throws IOException {
					out.write(getValue().getBytes("UTF-8"));
				}
			}

			public BodyItemType getType() {
				return type;
			}

			public String getFieldName() {
				return fieldName;
			}

			public void setFieldName(String fieldName) {
				this.fieldName = fieldName;
			}

			public T getValue() {
				return value;
			}

			public void setValue(T value) {
				this.value = value;
			}
		}

		public List<BodyItem> getBodyItems() {
			return bodyItems;
		}

		public void setBodyItems(List<BodyItem> bodyItems) {
			this.bodyItems = bodyItems;
		}
	}


}
