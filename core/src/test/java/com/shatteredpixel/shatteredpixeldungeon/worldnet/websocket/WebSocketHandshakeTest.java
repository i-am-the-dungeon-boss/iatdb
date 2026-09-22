package com.shatteredpixel.shatteredpixeldungeon.worldnet.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The upgrade request, and what the client will accept as an answer to it.
 *
 * <p>A handshake that is subtly wrong does not fail visibly: the socket
 * connects, and then nothing ever arrives. These pin the parts that would
 * otherwise only be checked by a real server.
 */
@DisplayName("World socket handshake")
class WebSocketHandshakeTest {

	private static final URI TARGET = URI.create("wss://echoes.example.com/v1/world/socket?instance=0");

	private static Map<String, String> headers() {
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put("Authorization", "Bearer token-value");
		return headers;
	}

	/** A handshake exchange against a canned server response. */
	private static void perform(String response) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)));
		WebSocketHandshake.perform(in, new ByteArrayOutputStream(), TARGET, headers(), new SecureRandom());
	}

	@Test
	@DisplayName("asks to upgrade the path and query, not the whole URL")
	void requestsTheOriginForm() {
		assertThat(WebSocketHandshake.request(TARGET, "key", headers()))
				.startsWith("GET /v1/world/socket?instance=0 HTTP/1.1\r\n")
				.contains("Host: echoes.example.com\r\n")
				.contains("Upgrade: websocket\r\n")
				.contains("Sec-WebSocket-Version: 13\r\n")
				.endsWith("\r\n\r\n");
	}

	@Test
	@DisplayName("carries credentials in headers, never in the query string")
	void credentialsTravelInHeaders() {
		String request = WebSocketHandshake.request(TARGET, "key", headers());

		assertThat(request).contains("Authorization: Bearer token-value\r\n");
		// Access logs and proxy caches keep query strings; they must not keep this.
		assertThat(request.substring(0, request.indexOf("\r\n"))).doesNotContain("token-value");
	}

	@Test
	@DisplayName("asks for a root path when the URL has none")
	void aPathlessUrlStillAsksForSomething() {
		assertThat(WebSocketHandshake.request(URI.create("ws://localhost:3001"), "key", headers()))
				.startsWith("GET / HTTP/1.1\r\n");
	}

	@Test
	@DisplayName("derives the accept value the specification's own example gives")
	void matchesTheSpecificationsWorkedExample() throws IOException {
		// RFC 6455 §1.3.
		assertThat(WebSocketHandshake.expectedAccept("dGhlIHNhbXBsZSBub25jZQ=="))
				.isEqualTo("s3pPLMBiTxaQ9kYGzzhZRbK+xOo=");
	}

	@Test
	@DisplayName("refuses anything that is not a 101, rather than reading frames from it")
	void anythingButAnUpgradeIsRefused() {
		assertThatThrownBy(() -> perform("HTTP/1.1 401 Unauthorized\r\n\r\n"))
				.isInstanceOf(IOException.class)
				.hasMessageContaining("Upgrade refused");
	}

	@Test
	@DisplayName("refuses a 101 whose accept value was not derived from our key")
	void a101WithoutTheDerivedValueIsNotWebSocket() {
		// A proxy answering 101 on its own account would otherwise be believed.
		assertThatThrownBy(() -> perform("HTTP/1.1 101 Switching Protocols\r\n"
				+ "Upgrade: websocket\r\n"
				+ "Sec-WebSocket-Accept: not-derived-from-anything\r\n\r\n"))
				.isInstanceOf(IOException.class)
				.hasMessageContaining("accept mismatch");
	}

	@Test
	@DisplayName("refuses a 101 that omits the accept header entirely")
	void aMissingAcceptHeaderIsNotForgiven() {
		assertThatThrownBy(() -> perform("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\n\r\n"))
				.isInstanceOf(IOException.class)
				.hasMessageContaining("accept mismatch");
	}

	@Test
	@DisplayName("refuses a server that hangs up before answering")
	void anEmptyAnswerIsRefused() {
		assertThatThrownBy(() -> perform(""))
				.isInstanceOf(IOException.class)
				.hasMessageContaining("Upgrade refused");
	}
}
