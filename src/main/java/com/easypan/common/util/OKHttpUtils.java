package com.easypan.common.util;

import com.easypan.common.response.ResponseCodeEnum;
import com.easypan.common.exception.BusinessException;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class OKHttpUtils {

    private static final Logger logger = LoggerFactory.getLogger(OKHttpUtils.class);

    /** 是否打印请求/响应的 body，建议生产环境设为 false */
    private static final boolean LOG_BODY = false;

    /** 超时时间（秒） */
    private static final int TIME_OUT_SECONDS = 8;

    /** 最大连接数 & 保活时间，可根据业务压测调整 */
    private static final int MAX_IDLE_CONNECTIONS = 20;
    private static final long KEEP_ALIVE_DURATION_MINUTES = 5L;

    private static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .followRedirects(false)
            .retryOnConnectionFailure(false)
            .connectTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(
                    MAX_IDLE_CONNECTIONS,
                    KEEP_ALIVE_DURATION_MINUTES,
                    TimeUnit.MINUTES
            ))
            .build();

    private OKHttpUtils() {}

    // ======================= GET =======================

    /** GET 请求 */
    public static String getRequest(String url, Map<String, String> headers) throws BusinessException {
        Request.Builder requestBuilder = new Request.Builder().url(url).get();
        addHeaders(requestBuilder, headers);
        Request request = requestBuilder.build();

        try (Response response = client.newCall(request).execute()) {
            String result = readBodyAsString(response);

            if (LOG_BODY && logger.isInfoEnabled()) {
                logger.info("OKHttp GET URL: {}, headers: {}, 返回: {}", url, headers, result);
            } else {
                logger.info("OKHttp GET URL: {}, headers: {}, status: {}", url, headers, response.code());
            }

            if (!response.isSuccessful()) {
                // 这里可以根据不同 status code 转成不同业务异常
                throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
            }

            return result;
        } catch (SocketTimeoutException | ConnectException e) {
            logger.error("OKHttp GET 请求超时, URL: {}", url, e);
            throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
        } catch (Exception e) {
            logger.error("OKHttp GET 请求异常, URL: {}", url, e);
            throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
        }
    }

    // ======================= POST - FORM =======================

    /** POST 表单请求 */
    public static String postForm(String url,
                                  Map<String, String> params,
                                  Map<String, String> headers) throws BusinessException {
        FormBody formBody = buildFormBody(params);
        return post(url, formBody, headers, "POST_FORM", params);
    }

    // ======================= POST - JSON =======================

    /** POST JSON 请求 */
    public static String postJson(String url,
                                  String jsonBody,
                                  Map<String, String> headers) throws BusinessException {
        if (jsonBody == null) {
            jsonBody = "";
        }
        RequestBody body = RequestBody.create(jsonBody.getBytes(StandardCharsets.UTF_8), JSON);
        return post(url, body, headers, "POST_JSON", jsonBody);
    }

    // ======================= Private 公共 POST =======================

    private static String post(String url,
                               RequestBody body,
                               Map<String, String> headers,
                               String type,
                               Object logParams) throws BusinessException {

        Request.Builder requestBuilder = new Request.Builder().url(url).post(body);
        addHeaders(requestBuilder, headers);
        Request request = requestBuilder.build();

        try (Response response = client.newCall(request).execute()) {
            String result = readBodyAsString(response);

            if (LOG_BODY && logger.isInfoEnabled()) {
                logger.info("OKHttp {} URL: {}, headers: {}, 参数: {}, 返回: {}",
                        type, url, headers, logParams, result);
            } else {
                logger.info("OKHttp {} URL: {}, headers: {}, 参数: {}, status: {}",
                        type, url, headers, logParams, response.code());
            }

            if (!response.isSuccessful()) {
                throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
            }

            return result;
        } catch (SocketTimeoutException | ConnectException e) {
            logger.error("OKHttp {} 请求超时, URL: {}, 参数: {}", type, url, logParams, e);
            throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
        } catch (Exception e) {
            logger.error("OKHttp {} 请求异常, URL: {}, 参数: {}", type, url, logParams, e);
            throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
        }
    }

    // ======================= 辅助方法 =======================

    /** 添加请求头（null 值转成空字符串） */
    private static void addHeaders(Request.Builder builder, Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            headers.forEach((k, v) -> builder.addHeader(k, v == null ? "" : v));
        }
    }

    /** 构建表单请求体 */
    private static FormBody buildFormBody(Map<String, String> params) {
        FormBody.Builder builder = new FormBody.Builder();
        if (params != null && !params.isEmpty()) {
            params.forEach((k, v) -> builder.add(k, v == null ? "" : v));
        }
        return builder.build();
    }

    /** 安全读取响应 body 为字符串（可能为 null） */
    private static String readBodyAsString(Response response) throws Exception {
        ResponseBody body = response.body();
        if (body == null) {
            return null;
        }
        return body.string();
    }
}
