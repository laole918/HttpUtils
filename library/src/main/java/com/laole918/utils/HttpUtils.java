package com.laole918.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by laole918 on 2016/6/3 0003.
 */
public class HttpUtils {

	private final static int CONNECT_TIMEOUT = 15 * 1000;
	private final static int READ_TIMEOUT = 15 * 1000;
	private final static String CHARSET = "UTF-8";
	private final static String GET = "GET";
	private final static String POST = "POST";

	public static RequestBuilder request(String url) {
		return new RequestBuilder(url);
	}

	private static Object request(RequestBuilder builder) {
		HttpURLConnection connection = null;
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		BufferedReader reader = null;
		BufferedWriter writer = null;

		RandomAccessFile raf = null;
		try {
			if (GET.equals(builder.method)) {
				if (!isEmpty(builder.args)) {
					if (builder.url.contains("?")) {
						if (builder.url.endsWith("&")) {
							builder.url = builder.url + builder.args;
						} else {
							builder.url = builder.url + "&" + builder.args;
						}
					} else {
						builder.url = builder.url + "?" + builder.args;
					}
				}
			}
			URL mURL = new URL(builder.url);
			connection = (HttpURLConnection) mURL.openConnection();
			connection.setConnectTimeout(CONNECT_TIMEOUT);
			connection.setReadTimeout(READ_TIMEOUT);
			connection.setRequestMethod(builder.method);
			prepareRequestProperty(connection, builder.properties);

			if (builder.download != null && builder.download.downloadUnit != null) {
				int startBytes = builder.download.downloadUnit.startBytes;
				int endBytes = builder.download.downloadUnit.endBytes;
				connection.setRequestProperty("Range", "bytes=" + startBytes + "-" + ((endBytes > 0) ? endBytes : ""));
			}

			if (POST.equals(builder.method) && !isEmpty(builder.args)) {
				connection.setDoOutput(true);
				connection.setUseCaches(false);
				OutputStream out = connection.getOutputStream();
				writer = new BufferedWriter(new OutputStreamWriter(out, CHARSET));
				writer.write(builder.args);
				writer.flush();
			}
			int code = connection.getResponseCode();
			if (HttpURLConnection.HTTP_OK == code || (HttpURLConnection.HTTP_PARTIAL == code)) {
				if (builder.download != null && builder.download.downloadUnit == null) {
					return connection.getContentLength();
				}
				InputStream in = connection.getInputStream();
				if (builder.download != null) {
					bis = new BufferedInputStream(in);
					if (builder.download.downloadUnit.startBytes > 0 || builder.download.downloadUnit.endBytes > 0) {
						raf = new RandomAccessFile(builder.download.targetFile, "rw");
						raf.seek(builder.download.downloadUnit.startBytes);
					} else {
						bos = new BufferedOutputStream(new FileOutputStream(builder.download.targetFile));
					}
					downloadResult(builder, bis, bos, raf);
					if (builder.download.isSupportBreakpoint) {
						checkRangeFile(builder);
					}
					return true;
				} else {
					reader = new BufferedReader(new InputStreamReader(in, CHARSET));
					return normalResult(reader);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(bis);
			close(bos);
			close(reader);
			close(writer);
			close(raf);
			disconnect(connection);
		}
		return null;
	}

	private static String prepareParams(Map<String, String> args) {
		if (args != null && !args.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<String, String> a : args.entrySet()) {
				String key = String.valueOf(a.getKey());
				String value = String.valueOf(a.getValue());
				key = urlEncode(key);
				value = urlEncode(value);
				sb.append(key).append("=").append(value).append("&");
			}
			if (sb.length() > 0) {
				sb.setLength(sb.length() - 1);
			}
			return sb.toString();
		}
		return null;
	}

	private static void prepareRequestProperty(HttpURLConnection connection, Map<String, String> property) {
		if (property != null && !property.isEmpty()) {
			for (Map.Entry<String, String> p : property.entrySet()) {
				if (connection.getRequestProperty(p.getKey()) == null) {
					connection.setRequestProperty(p.getKey(), p.getValue());
				} else {
					connection.addRequestProperty(p.getKey(), p.getValue());
				}
			}
		}
	}

	private static void downloadResult(RequestBuilder builder, BufferedInputStream bis, BufferedOutputStream bos,
			RandomAccessFile raf) throws IOException {
		int totalLength = 0;
		try {
			int length;
			byte[] buffer = new byte[1024 * 4];
			while ((length = bis.read(buffer)) > 0 && length != -1) {
				if (bos != null) {
					bos.write(buffer, 0, length);
				} else if (raf != null) {
					raf.write(buffer, 0, length);
				}
				totalLength += length;
			}
		} catch (Exception e) {
			if (builder.download.isSupportBreakpoint) {
				writeUnitConfig(builder, totalLength);
			}
			throw e;
		}
	}

	private static String normalResult(BufferedReader reader) throws Exception {
		int length;
		char[] buffer = new char[1024 * 2];
		StringBuilder sb = new StringBuilder();
		while ((length = reader.read(buffer)) > 0 && length != -1) {
			sb.append(buffer, 0, length);
		}
		return sb.toString();
	}

	private static void disconnect(HttpURLConnection connection) {
		if (connection != null) {
			connection.disconnect();
		}
	}

	private static void close(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static String urlEncode(String str) {
		try {
			return URLEncoder.encode(str, CHARSET);
		} catch (Exception e) {
			return URLEncoder.encode(str);
		}
	}

	private static boolean isEmpty(CharSequence str) {
		return str == null || str.length() == 0;
	}

	public static class RequestBuilder {

		private String method = GET;
		private String url;
		private String args;
		private Map<String, String> properties;
		private Map<String, String> params;
		private DownloadBuilder download;

		public RequestBuilder(String url) {
			this.url = url;
		}

		public RequestBuilder args(String args) {
			this.args = args;
			return this;
		}

		public RequestBuilder params(Map<String, String> params) {
			if (this.params == null) {
				this.params = params;
			} else {
				this.params.putAll(params);
			}
			return this;
		}

		public RequestBuilder param(String key, String value) {
			if (this.params == null) {
				this.params = new HashMap<>();
			}
			this.params.put(key, value);
			return this;
		}

		public RequestBuilder properties(Map<String, String> properties) {
			if (this.properties == null) {
				this.properties = properties;
			} else {
				this.properties.putAll(properties);
			}
			return this;
		}

		public RequestBuilder property(String key, String value) {
			if (this.properties == null) {
				this.properties = new HashMap<>();
			}
			this.properties.put(key, value);
			return this;
		}

		private Object toRequest() {
			if (isEmpty(url)) {
				return null;
			} else {
				if (isEmpty(this.args)) {
					this.args = prepareParams(params);
					this.params = null;
				}
				return request(this);
			}
		}

		public String get() {
			this.method = GET;
			Object obj = toRequest();
			if (obj instanceof String) {
				return (String) obj;
			} else {
				return null;
			}
		}

		public String post() {
			this.method = POST;
			Object obj = toRequest();
			if (obj instanceof String) {
				return (String) obj;
			} else {
				return null;
			}
		}

		public String json(String json) {
			this.args = json;
			property("Content-Type", "application/json; charset=" + CHARSET);
			return post();
		}

		public DownloadBuilder download() {
			download = new DownloadBuilder(this);
			return download;
		}
	}

	public static class DownloadBuilder {

		private RequestBuilder mRequestBuilder;
		private File targetFile;
		private String targetFilePath;
		private String targetFileName;

		private DownloadUnit downloadUnit;

		private boolean isSupportBreakpoint;// 是否支持断点续传

		private DownloadBuilder(RequestBuilder requestBuilder) {
			mRequestBuilder = requestBuilder;
			isSupportBreakpoint = false;
		}

		public DownloadBuilder target(File target) {
			targetFile = target;
			return this;
		}

		public DownloadBuilder targetPath(String path) {
			targetFilePath = path;
			return this;
		}

		public DownloadBuilder targetPath(File path) {
			return targetPath(path == null ? null : path.getAbsolutePath());
		}

		public DownloadBuilder targetName(String name) {
			targetFileName = name;
			return this;
		}

		public DownloadBuilder breakpoint(boolean breakpoint) {
			isSupportBreakpoint = breakpoint;
			return this;
		}

		public DownloadUnit[] units(int units) {
			if (!getTargetFile()) {
				return null;
			}
			DownloadUnit[] downloadUnits;
			File rangeFile = null;
			if (isSupportBreakpoint) {
				rangeFile = new File(targetFile.getAbsolutePath() + ".range");
				if (rangeFile.exists()) {
					downloadUnits = readUnitsConfig(rangeFile);
					if (downloadUnits != null) {
						return downloadUnits;
					}
				}
			}
			int length;
			if(units <= 1) {
				length = -1;
			} else {
				length = getContentLength();
				if(length < 0) {
					return null;
				}
			}
			downloadUnits = createDownloadUnits(length, units);
			if (isSupportBreakpoint) {
				createUnitsConfig(rangeFile, downloadUnits);
			}
			return downloadUnits;
		}

		public boolean downloadUnit(DownloadUnit du) {
			downloadUnit = du;
			return toDownload();
		}

		public boolean downloadInOneUnit() {
			DownloadUnit[] downloadUnits = units(1);
			if (downloadUnits != null) {
				downloadUnit = downloadUnits[0];
				return toDownload();
			} else {
				return false;
			}
		}

		private int getContentLength() {
			mRequestBuilder.method = GET;
			Object obj = mRequestBuilder.toRequest();
			if (obj instanceof Integer) {
				return (int) obj;
			} else {
				return -1;
			}
		}

		private boolean getTargetFile() {
			if (targetFile == null) {
				if (isEmpty(targetFilePath) || isEmpty(targetFileName)) {
					return false;
				}
				targetFile = new File(targetFilePath, targetFileName);
				targetFilePath = targetFileName = null;
			} else {
				if (!isEmpty(targetFilePath) && !isEmpty(targetFileName)) {
					targetFile = new File(targetFilePath, targetFileName);
					targetFilePath = targetFileName = null;
				}
			}
			File path = targetFile.getParentFile();
			if (!path.exists() && !path.mkdirs()) {
				return false;
			}
			return true;
		}

		private boolean toDownload() {
			if (!getTargetFile()) {
				return false;
			}
			mRequestBuilder.method = GET;
			Object obj = mRequestBuilder.toRequest();
			if (obj instanceof Boolean) {
				return (boolean) obj;
			} else {
				return false;
			}
		}
	}

	public static class DownloadUnit {
		public int id = -1;
		public int startBytes = -1;
		public int endBytes = -1;
	}

	private static DownloadUnit[] createDownloadUnits(int length, int units) {
		if (units > 1) {
			int unitLength = length / units;
			if (unitLength > 0) {
				int unitOverLength = length % units;
				DownloadUnit[] downloadUnits = new DownloadUnit[units];
				for (int i = 0, j = 0; i < length && j < units; i += unitLength, j++) {
					DownloadUnit du = new DownloadUnit();
					du.id = j;
					du.startBytes = i;
					du.endBytes = du.startBytes + unitLength;
					downloadUnits[j] = du;
				}
				downloadUnits[units - 1].endBytes += unitOverLength;
				return downloadUnits;
			} else {
				DownloadUnit du = createOneDownloadUnit();
				return new DownloadUnit[] { du };
			}
		} else {
			DownloadUnit du = createOneDownloadUnit();
			return new DownloadUnit[] { du };
		}
	}

	private static DownloadUnit createOneDownloadUnit() {
		DownloadUnit du = new DownloadUnit();
		du.id = 0;
		du.startBytes = 0;
		du.endBytes = -1;
		return du;
	}

	private static void createUnitsConfig(File rangeFile, DownloadUnit[] downloadUnits) {
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(rangeFile, "rw");
			if (downloadUnits != null) {
				raf.writeInt(downloadUnits.length);
				for (DownloadUnit du : downloadUnits) {
					raf.writeInt(du.startBytes);
					raf.writeInt(du.endBytes);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(raf);
		}
	}

	private static DownloadUnit[] readUnitsConfig(File rangeFile) {
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(rangeFile, "r");
			int units = raf.readInt();
			DownloadUnit[] downloadUnits = new DownloadUnit[units];
			for (int i = 0; i < units; i++) {
				DownloadUnit du = new DownloadUnit();
				du.id = i;
				du.startBytes = raf.readInt();
				du.endBytes = raf.readInt();
				downloadUnits[i] = du;
			}
			return downloadUnits;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(raf);
		}
		return null;
	}

	private static void writeUnitConfig(RequestBuilder builder, int length) {
		File rangeFile = new File(builder.download.targetFile.getAbsolutePath() + ".range");
		RandomAccessFile raf = null;
		try {
			int id = builder.download.downloadUnit.id;
			int nowLength = builder.download.downloadUnit.startBytes + length;
			raf = new RandomAccessFile(rangeFile, "rw");
			raf.seek(4 + (id * 8));
			raf.writeInt(nowLength);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(raf);
		}
	}

	private static void checkRangeFile(RequestBuilder builder) {
		File rangeFile = new File(builder.download.targetFile.getAbsolutePath() + ".range");
		RandomAccessFile raf = null;
		try {
			int id = builder.download.downloadUnit.id;
			int nowLength = builder.download.downloadUnit.endBytes;
			raf = new RandomAccessFile(rangeFile, "rw");
			raf.seek(4 + (id * 8));
			raf.writeInt(nowLength);

			raf.seek(0);
			int units = raf.readInt();
			for (int i = 0; i < units; i++) {
				if (i != id) {
					int startBytes = raf.readInt();
					int endBytes = raf.readInt();
					if (startBytes < endBytes) {
						return;
					}
				} else {
					raf.skipBytes(8);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(raf);
		}
		rangeFile.delete();
	}
}
