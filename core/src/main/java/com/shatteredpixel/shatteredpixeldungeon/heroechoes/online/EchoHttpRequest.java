package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import java.util.Map;

public final class EchoHttpRequest {

	public final String method;
	public final String url;
	public final Map<String, String> headers;
	public final String body;

	public EchoHttpRequest(String method, String url, Map<String, String> headers, String body) {
		this.method = method;
		this.url = url;
		this.headers = headers;
		this.body = body;
	}
}
