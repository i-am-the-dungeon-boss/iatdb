package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

public interface EchoHttpTransport {

	EchoHttpResponse send(EchoHttpRequest request) throws Exception;
}
