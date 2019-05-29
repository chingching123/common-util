/**
 * 
 */
/**
 * @author Administrator
 *
 */
package com.fpx.common.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;


public class HttpClientPoolUtil {
	protected static Logger logger = LoggerFactory.getLogger(HttpClientPoolUtil.class);
	private static HttpClientPoolUtil instance = null;
	private static PoolingHttpClientConnectionManager cm;
	private static HttpRequestRetryHandler httpRequestRetryHandler;
	public static final String DEFAULT_CHARSET = "UTF-8";
	public static final String APPLICATION_JSON = "application/json";
	private static String httpAddr = "";


	public HttpClientPoolUtil() {
		ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
		LayeredConnectionSocketFactory sslsf = SSLConnectionSocketFactory.getSocketFactory();
		Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory> create()
				.register("http", plainsf).register("https", sslsf).build();
		cm = new PoolingHttpClientConnectionManager(registry);
		// 将最大连接数增加到200
		cm.setMaxTotal(200);
		// 将每个路由基础的连接增加到20
		cm.setDefaultMaxPerRoute(20);

		// 请求重试处理
		httpRequestRetryHandler = new HttpRequestRetryHandler() {
			@Override
			public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
				if (executionCount >= 3) {// 如果已经重试了3次，就放弃
					return false;
				}
				if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
					return true;
				}
				if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常
					return false;
				}
				if (exception instanceof InterruptedIOException) {// 超时
					return false;
				}
				if (exception instanceof UnknownHostException) {// 目标服务器不可达
					return false;
				}
				if (exception instanceof ConnectTimeoutException) {// 连接被拒绝
					return false;
				}
				if (exception instanceof SSLException) {// ssl握手异常
					return false;
				}

				HttpClientContext clientContext = HttpClientContext.adapt(context);
				HttpRequest request = clientContext.getRequest();
				// 如果请求是幂等的，就再次尝试
				if (!(request instanceof HttpEntityEnclosingRequest)) {
					return true;
				}
				return false;
			}
		};
		// 定时清楚过期和闲置连接
		ThreadFactory namedThreadFactory = Executors.defaultThreadFactory();
		ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1, namedThreadFactory);
		scheduler.scheduleAtFixedRate(new IdleConnectionMonitor(cm), 10, 60, TimeUnit.MINUTES);

	}

	private final class IdleConnectionMonitor implements Runnable {
		PoolingHttpClientConnectionManager connectionManager;

		public IdleConnectionMonitor(PoolingHttpClientConnectionManager connectionManager) {
			this.connectionManager = connectionManager;
		}

		@Override
		public void run() {
			if (logger.isDebugEnabled()) {
				// logger.debug("release start connect count:=" +
				// connectionManager.getTotalStats().getAvailable());
			}
			// Close expired connections
			connectionManager.closeExpiredConnections();
			// Optionally, close connections
			// 可选的, 关闭30秒内不活动的连接
			connectionManager.closeIdleConnections(30, TimeUnit.SECONDS);

			if (logger.isDebugEnabled()) {
				// logger.debug("release end connect count:=" +
				// connectionManager.getTotalStats().getAvailable());
			}

		}
	}

	private static void config(HttpRequestBase httpRequestBase) {
		httpRequestBase.setHeader("User-Agent", "Mozilla/5.0");
		httpRequestBase.setHeader("Accept",
				"application/json,text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		httpRequestBase.setHeader("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");// "en-US,en;q=0.5");
		httpRequestBase.setHeader("Accept-Charset", "ISO-8859-1,utf-8,gbk,gb2312;q=0.7,*;q=0.7");
		httpRequestBase.setHeader("Accept-Encoding", "gzip, deflate");
		// httpRequestBase.setHeader("ContentType","application/x-www-form-urlencoded;charset=utf-8");

		// 配置请求的超时设置
		RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(5000).setConnectTimeout(5000)
				.setSocketTimeout(5000).build();
		httpRequestBase.setConfig(requestConfig);
	}

	/**
	 * 发送 post请求
	 * 
	 * @param httpUrl
	 *            地址
	 * @throws Exception
	 */
	public static String sendHttpPost(String httpUrl) throws Exception {
		HttpPost httpPost = new HttpPost(httpAddr + httpUrl);// 创建httpPost
		return sendHttpPost(httpPost, DEFAULT_CHARSET);
	}

	/**
	 * 发送 post请求
	 * 
	 * @param httpUrl
	 *            地址
	 * @param params
	 *            参数(格式:key1=value1&key2=value2)
	 * @throws Exception
	 */
	public static String sendHttpPost(String httpUrl, String params) throws Exception {
		HttpPost httpPost = new HttpPost(httpAddr + httpUrl);// 创建httpPost
		// 设置参数
		StringEntity stringEntity = new StringEntity(params, DEFAULT_CHARSET);
		// stringEntity.setContentType("application/x-www-form-urlencoded;charset=utf-8");
		stringEntity.setContentType(APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		return sendHttpPost(httpPost, DEFAULT_CHARSET);
	}

	public static String sendHttpPost(String httpUrl, String params, String contentType) throws Exception {
		HttpPost httpPost = new HttpPost(httpAddr + httpUrl);// 创建httpPost
		// 设置参数
		StringEntity stringEntity = new StringEntity(params, DEFAULT_CHARSET);
		// stringEntity.setContentType("application/x-www-form-urlencoded");
		stringEntity.setContentType(contentType);
		httpPost.setEntity(stringEntity);
		return sendHttpPost(httpPost, DEFAULT_CHARSET);
	}

	public static String sendHttpPost(String httpUrl, String params, String contentType, String charset) throws Exception {
		HttpPost httpPost = new HttpPost(httpAddr + httpUrl);// 创建httpPost
		// 设置参数
		StringEntity stringEntity = new StringEntity(params, charset);
		// stringEntity.setContentType("application/x-www-form-urlencoded");
		stringEntity.setContentType(contentType);
		httpPost.setEntity(stringEntity);
		return sendHttpPost(httpPost, charset);
	}

	/**
	 * 发送 post请求
	 * 
	 * @param httpUrl
	 *            地址
	 * @param maps
	 *            参数
	 * @throws Exception
	 */
	public static String sendHttpPost(String httpUrl, Map<String, Object> maps) throws Exception {
		HttpPost httpPost = new HttpPost(httpAddr + httpUrl);// 创建httpPost
		// 创建参数队列
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		for (String key : maps.keySet()) {
			nameValuePairs.add(new BasicNameValuePair(key, maps.get(key).toString()));
		}

		httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, DEFAULT_CHARSET));

		return sendHttpPost(httpPost, DEFAULT_CHARSET);
	}

	public static String sendMultiHttpPost(String httpUrl, Map<String, Object> maps, Map<String, byte[]> fileMap)
			throws Exception {
		HttpPost httpPost = new HttpPost(httpAddr + httpUrl);// 创建httpPost
		String boundary = "-------------" + System.currentTimeMillis();
		httpPost.setHeader("Content-type", "multipart/form-data; boundary=" + boundary);
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		builder.setBoundary(boundary);
		for (Map.Entry<String, Object> entry : maps.entrySet()) {
			StringBody stringBody = new StringBody(entry.getValue().toString(),
					ContentType.create("text/plain", Consts.UTF_8));
			builder.addPart(entry.getKey(), stringBody);
		}
		for (Map.Entry<String, byte[]> entry : fileMap.entrySet()) {
			ByteArrayBody byteArrayBody = new ByteArrayBody((byte[]) entry.getValue(), entry.getKey());
			builder.addPart(entry.getKey(), byteArrayBody);
		}
		HttpEntity reqEntity = builder.build();

		httpPost.setEntity(reqEntity);

		return sendHttpPost(httpPost, DEFAULT_CHARSET);
	}

	public static String sendSSLHttpPost(String httpUrl, String params, String contentType, String certPath, String certPwd)
			throws Exception {
		HttpPost httpPost = new HttpPost(httpAddr + httpUrl);// 创建httpPost
		// 设置参数
		StringEntity stringEntity = new StringEntity(params, DEFAULT_CHARSET);
		// stringEntity.setContentType("application/x-www-form-urlencoded");
		stringEntity.setContentType(contentType);
		httpPost.setEntity(stringEntity);
		return sendSSLHttpPost(httpPost, DEFAULT_CHARSET, certPath, certPwd);
	}

	/**
	 * 发送Post请求
	 * 
	 * @param httpPost
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	private static String sendSSLHttpPost(HttpPost httpPost, String charset, String certPath, String certPwd)
			throws Exception {

		FileInputStream instream = null;
		CloseableHttpClient httpClient = null;
		CloseableHttpResponse response = null;
		HttpEntity entity = null;
		String responseContent = null;
		try {
			instream = new FileInputStream(new File(certPath));
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(instream, certPwd.toCharArray());
			// Trust own CA and all self-signed certs
			SSLContext sslcontext = SSLContexts.custom().loadKeyMaterial(keyStore, certPwd.toCharArray()).build();
			// Allow TLSv1 protocol only
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new String[] { "TLSv1" },
					null, SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);

			// try {
			// 创建默认的httpClient实例.
			httpClient = HttpClients.custom()
					// .setConnectionManager(cm)
					.setSSLSocketFactory(sslsf)
					// .setSSLContext(sslcontext)
					.setRetryHandler(httpRequestRetryHandler).build();

			config(httpPost);
			// 执行请求
			response = httpClient.execute(httpPost);
			entity = response.getEntity();
			responseContent = EntityUtils.toString(entity, charset);
			int status = response.getStatusLine().getStatusCode();
			if (status != HttpStatus.SC_OK) {
				throwRemoteException(status);
			}
		} catch (Exception e) {
			if (httpPost != null) {
				httpPost.abort();
			}
			throw e;
		} finally {
			if (instream != null) {
				instream.close();
			}
			// 关闭连接,释放资源
			if (response != null) {
				response.close();
			}
		}
		return responseContent;
	}

	private static void throwRemoteException(int status) throws RuntimeException {
		String msg = "调用远程服务出错".concat(String.valueOf(status));
		throw new RuntimeException(msg);
	}

	/**
	 * 发送Post请求
	 * 
	 * @param httpPost
	 * @return
	 * @throws Exception
	 */
	private static String sendHttpPost(HttpPost httpPost, String charset) throws Exception {
		CloseableHttpClient httpClient = null;
		CloseableHttpResponse response = null;
		HttpEntity entity = null;
		String responseContent = null;
		try {
			// 创建默认的httpClient实例.
			httpClient = HttpClients.custom().setConnectionManager(cm).setRetryHandler(httpRequestRetryHandler).build();

			// httpClient = HttpClients.createDefault();
			config(httpPost);
			// 执行请求
			response = httpClient.execute(httpPost);
			entity = response.getEntity();
			responseContent = EntityUtils.toString(entity, charset);
			int status = response.getStatusLine().getStatusCode();
			if (status != HttpStatus.SC_OK) {
				throwRemoteException(status);
			}
		} catch (Exception e) {
			if (httpPost != null) {
				httpPost.abort();
			}
			throw e;
		} finally {
			try {
				// 关闭连接,释放资源
				if (response != null) {
					response.close();
				}
			} catch (IOException e) {
				logger.error("", e);
			}
		}
		return responseContent;
	}

	/**
	 * 发送 get请求
	 * 
	 * @param httpUrl
	 * @throws Exception
	 */
	public static String sendHttpGet(String httpUrl) throws Exception {
		HttpGet httpGet = null;
		// url已经包含了域名就不需要加入默认的域名前缀
		boolean ishttp = StringUtils.isNotBlank(httpUrl)
				&& (httpUrl.contains("http://") || httpUrl.contains("https://"));
		if (ishttp) {
			httpGet = new HttpGet(httpUrl);// 创建get请求
		} else {
			httpGet = new HttpGet(httpAddr + httpUrl);// 创建get请求
		}
		return sendHttpGet(httpGet, DEFAULT_CHARSET);
	}

	/**
	 * 发送 get请求Https
	 * 
	 * @param httpUrl
	 * @throws Exception
	 */
	// public String sendHttpsGet(String httpUrl) throws Exception {
	// HttpGet httpGet = new HttpGet(httpUrl);// 创建get请求
	// return sendHttpsGet(httpGet,DEFAULT_CHARSET);
	// }

	/**
	 * 发送Get请求
	 * 
	 * @param httpPost
	 * @return
	 * @throws Exception
	 */
	private static String sendHttpGet(HttpGet httpGet, String charset) throws Exception {
		CloseableHttpClient httpClient = null;
		CloseableHttpResponse response = null;
		HttpEntity entity = null;
		String responseContent = null;
		try {
			// 创建默认的httpClient实例.
			httpClient = HttpClients.custom().setConnectionManager(cm).setRetryHandler(httpRequestRetryHandler).build();
			config(httpGet);
			// 执行请求
			response = httpClient.execute(httpGet);
			entity = response.getEntity();
			responseContent = EntityUtils.toString(entity, charset);
			int status = response.getStatusLine().getStatusCode();
			if (status != HttpStatus.SC_OK) {
				throwRemoteException(status);
			}
		} catch (Exception e) {
			if (httpGet != null) {
				httpGet.abort();
			}
			throw e;
		} finally {
			try {
				// 关闭连接,释放资源
				if (response != null) {
					response.close();
				}
			} catch (IOException e) {
				logger.error("", e);
			}
		}
		return responseContent;
	}

	/**
	 * 发送Get请求Https
	 * 
	 * @param httpPost
	 * @return
	 * @throws Exception
	 */
	// private String sendHttpsGet(HttpGet httpGet,String charset) throws
	// Exception {
	// CloseableHttpClient httpClient = null;
	// CloseableHttpResponse response = null;
	// HttpEntity entity = null;
	// String responseContent = null;
	// try {
	// // 创建默认的httpClient实例.
	// PublicSuffixMatcher publicSuffixMatcher =
	// PublicSuffixMatcherLoader.load(new URL(httpGet.getURI().toString()));
	// DefaultHostnameVerifier hostnameVerifier = new
	// DefaultHostnameVerifier(publicSuffixMatcher);
	// httpClient = HttpClients.custom()
	// .setConnectionManager(cm)
	// .setRetryHandler(httpRequestRetryHandler)
	// .setSSLHostnameVerifier(hostnameVerifier)
	// .build();
	// config(httpGet);
	// // 执行请求
	// response = httpClient.execute(httpGet);
	// entity = response.getEntity();
	// responseContent = EntityUtils.toString(entity, charset);
	// int status = response.getStatusLine().getStatusCode();
	// if(status != HttpStatus.SC_OK){
	// String
	// msg=ErrorMessages.ERR_05004.getDescription().concat(String.valueOf(status));
	// throw new RemoteServiceException(ErrorMessages.ERR_05004.getCode(),msg);
	// }
	// } catch (Exception e) {
	// if(httpGet!=null)
	// httpGet.abort();
	// throw e;
	// } finally {
	// try {
	// // 关闭连接,释放资源
	// if (response != null) {
	// response.close();
	// }
	// } catch (IOException e) {
	// throw e;
	// }
	// }
	// return responseContent;
	// }


}
