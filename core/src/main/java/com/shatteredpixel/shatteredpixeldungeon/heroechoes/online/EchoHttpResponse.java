package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

public final class EchoHttpResponse {

	public final int statusCode;
	public final String body;

	public EchoHttpResponse(int statusCode, String body) {
		this.statusCode = statusCode;
		this.body = body != null ? body : "";
	}
}
