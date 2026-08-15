/*
 * I am the Dungeon Boss
 * Copyright (C) 2026 Dungeon Boss
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.worldnet.websocket;

import com.watabou.utils.Base64Codec;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Map;

/**
 * The HTTP upgrade that turns a plain socket into a WebSocket.
 *
 * <p>Split out of {@link RawWorldSocket} because it is the one part of that
 * class with no state and no threads: text in, text out, and a single verdict at
 * the end. Kept together rather than inlined because getting any of it slightly
 * wrong fails in the same unhelpful way — a socket that connects and then never
 * says anything.
 *
 * <p>{@link #request} is separated from the exchange so the bytes that go on the
 * wire can be asserted directly, without a server to talk to.
 */
final class WebSocketHandshake {

	/** The GUID from RFC 6455 §1.3, used to derive the handshake's accept value. */
	private static final String HANDSHAKE_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	/** A header line longer than this is a server misbehaving, not a header. */
	private static final int MAX_HEADER_LINE = 8192;

	private WebSocketHandshake() {
	}

	/**
	 * Upgrades the connection, or throws explaining why it could not be.
	 *
	 * <p>Fails loudly on anything short of a complete, correctly derived upgrade:
	 * a proxy that answers 101 without the accept value is not speaking WebSocket
	 * at us, and reading frames from it would be nonsense.
	 */
	static void perform(
			DataInputStream in, OutputStream out, URI target, Map<String, String> headers, SecureRandom random)
			throws IOException {
		byte[] nonce = new byte[16];
		random.nextBytes(nonce);
		String key = Base64Codec.encode(nonce);

		out.write(request(target, key, headers).getBytes(StandardCharsets.UTF_8));
		out.flush();

		String status = readHeaderLine(in);
		if (status == null || !status.contains(" 101")) {
			throw new IOException("Upgrade refused: " + status);
		}
		String accept = null;
		String line;
		while ((line = readHeaderLine(in)) != null && !line.isEmpty()) {
			int colon = line.indexOf(':');
			if (colon > 0 && "sec-websocket-accept".equals(line.substring(0, colon).trim().toLowerCase())) {
				accept = line.substring(colon + 1).trim();
			}
		}
		if (accept == null || !accept.equals(expectedAccept(key))) {
			throw new IOException("Handshake accept mismatch");
		}
	}

	/**
	 * The upgrade request as it goes on the wire.
	 *
	 * <p>The caller's headers are appended last and carry the credentials — the
	 * same ones the HTTP routes send. A token in the query string would end up in
	 * access logs and proxy caches, so it travels here instead.
	 */
	static String request(URI target, String key, Map<String, String> headers) {
		String path = target.getRawPath() == null || target.getRawPath().isEmpty() ? "/" : target.getRawPath();
		if (target.getRawQuery() != null) {
			path = path + "?" + target.getRawQuery();
		}
		StringBuilder request = new StringBuilder();
		request.append("GET ").append(path).append(" HTTP/1.1\r\n");
		request.append("Host: ").append(target.getHost()).append("\r\n");
		request.append("Upgrade: websocket\r\n");
		request.append("Connection: Upgrade\r\n");
		request.append("Sec-WebSocket-Key: ").append(key).append("\r\n");
		request.append("Sec-WebSocket-Version: 13\r\n");
		for (Map.Entry<String, String> header : headers.entrySet()) {
			request.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
		}
		request.append("\r\n");
		return request.toString();
	}

	static String expectedAccept(String key) throws IOException {
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			return Base64Codec.encode(sha1.digest((key + HANDSHAKE_GUID).getBytes(StandardCharsets.UTF_8)));
		} catch (java.security.NoSuchAlgorithmException absent) {
			throw new IOException("SHA-1 unavailable", absent);
		}
	}

	/** Reads one CRLF-terminated line without buffering past it. */
	private static String readHeaderLine(DataInputStream in) throws IOException {
		StringBuilder line = new StringBuilder();
		int previous = -1;
		while (true) {
			int next = in.read();
			if (next == -1) {
				return line.length() == 0 ? null : line.toString();
			}
			if (previous == '\r' && next == '\n') {
				line.setLength(Math.max(0, line.length() - 1));
				return line.toString();
			}
			line.append((char) next);
			previous = next;
			if (line.length() > MAX_HEADER_LINE) {
				throw new IOException("Header line too long");
			}
		}
	}
}
