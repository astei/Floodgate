/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Floodgate
 */

package org.geysermc.floodgate.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@SuppressWarnings("all")
public class HttpUtils {
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

    private static final Gson GSON = new Gson();
    private static final String USER_AGENT = "GeyserMC/Floodgate";
    private static final String CONNECTION_STRING = "--";
    private static final String BOUNDARY = "******";
    private static final String END = "\r\n";

    public static CompletableFuture<DefaultHttpResponse> asyncGet(String urlString) {
        return CompletableFuture.supplyAsync(() -> {
            return get(urlString);
        }, EXECUTOR_SERVICE);
    }

    public static DefaultHttpResponse get(String urlString) {
        return readDefaultResponse(request(urlString));
    }

    public static <T> HttpResponse<T> getSilent(String urlString, Class<T> clazz) {
        return readResponseSilent(request(urlString), clazz);
    }

    private static HttpURLConnection request(String urlString) {
        HttpURLConnection connection;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create connection", exception);
        }

        try {
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            connection.setRequestProperty("User-Agent", USER_AGENT);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create request", exception);
        }

        return connection;
    }

    private static <T> HttpResponse<T> readResponseSilent(HttpURLConnection connection, Class<T> clazz) {
        InputStreamReader streamReader = createReader(connection);

        try {
            int responseCode = connection.getResponseCode();
            T response = GSON.fromJson(streamReader, clazz);
            return new HttpResponse<>(responseCode, response);
        } catch (Exception ignored) {
            return new HttpResponse<>(-1, null);
        } finally {
            try {
                streamReader.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static DefaultHttpResponse readDefaultResponse(HttpURLConnection connection) {
        InputStreamReader streamReader = createReader(connection);

        try {
            int responseCode = connection.getResponseCode();
            JsonObject response = GSON.fromJson(streamReader, JsonObject.class);
            return new DefaultHttpResponse(responseCode, response);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to read response", exception);
        } finally {
            try {
                streamReader.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static InputStreamReader createReader(HttpURLConnection connection) {
        InputStream stream = null;
        try {
            stream = connection.getInputStream();
        } catch (Exception exception) {
            try {
                stream = connection.getErrorStream();
            } catch (Exception exception1) {
                throw new RuntimeException("Both the input and the error stream failed?!");
            }
        }
        return new InputStreamReader(stream);
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class HttpResponse<T> {
        private final int httpCode;
        private final T response;
    }

    public static final class DefaultHttpResponse extends HttpResponse<JsonObject> {
        private DefaultHttpResponse(int httpCode, JsonObject response) {
            super(httpCode, response);
        }
    }
}
