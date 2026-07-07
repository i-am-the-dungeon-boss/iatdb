package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class JavaEchoHttpTransport implements EchoHttpTransport {

	public static final int CONNECT_TIMEOUT_MS = 5000;
	public static final int READ_TIMEOUT_MS = 8000;

	@Override
	public EchoHttpResponse send(EchoHttpRequest request) throws Exception {
		HttpURLConnection connection = (HttpURLConnection) new URL(request.url).openConnection();
		connection.setRequestMethod(request.method);
		connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
		connection.setReadTimeout(READ_TIMEOUT_MS);
		connection.setDoInput(true);

		if (request.headers != null) {
			for (Map.Entry<String, String> header : request.headers.entrySet()) {
				connection.setRequestProperty(header.getKey(), header.getValue());
			}
		}

		if (request.body != null && !request.body.isEmpty()) {
			connection.setDoOutput(true);
			try (OutputStream out = connection.getOutputStream()) {
				out.write(request.body.getBytes(StandardCharsets.UTF_8));
			}
		}

		int status = connection.getResponseCode();
		InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
		String body = readBody(stream);
		connection.disconnect();
		return new EchoHttpResponse(status, body);
	}

	private static String readBody(InputStream stream) throws Exception {
		if (stream == null) {
			return "";
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			return sb.toString();
		}
	}
}
