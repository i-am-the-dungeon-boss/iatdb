package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

public final class EchoHttpException extends Exception {

	public final int statusCode;
	public final String responseBody;

	public EchoHttpException(int statusCode, String responseBody) {
		super("Echo API request failed with HTTP " + statusCode);
		this.statusCode = statusCode;
		this.responseBody = responseBody != null ? responseBody : "";
	}
}
