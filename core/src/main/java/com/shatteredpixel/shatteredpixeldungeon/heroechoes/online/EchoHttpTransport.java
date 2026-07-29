package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import java.util.Map;

public interface EchoHttpTransport {

	Response send(Request request) throws Exception;

	final class Request {
		public final String method;
		public final String url;
		public final Map<String, String> headers;
		public final String body;

		public Request(String method, String url, Map<String, String> headers, String body) {
			this.method = method;
			this.url = url;
			this.headers = headers;
			this.body = body;
		}
	}

	final class Response {
		public final int statusCode;
		public final String body;

		public Response(int statusCode, String body) {
			this.statusCode = statusCode;
			this.body = body != null ? body : "";
		}
	}

	final class HttpException extends Exception {
		public final int statusCode;
		public final String responseBody;

		public HttpException(int statusCode, String responseBody) {
			super("Echo API request failed with HTTP " + statusCode);
			this.statusCode = statusCode;
			this.responseBody = responseBody != null ? responseBody : "";
		}
	}
}
