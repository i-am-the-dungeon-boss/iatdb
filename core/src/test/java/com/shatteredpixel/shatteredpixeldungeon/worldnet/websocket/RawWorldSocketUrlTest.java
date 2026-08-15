package com.shatteredpixel.shatteredpixeldungeon.worldnet.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("World socket endpoint")
class RawWorldSocketUrlTest {

	@Test
	@DisplayName("upgrades an https backend to wss")
	void upgradesHttpsToWss() {
		assertThat(RawWorldSocket.socketUrl("https://echoes.example.com", ""))
				.isEqualTo("wss://echoes.example.com/v1/world/socket?instance=0");
	}

	@Test
	@DisplayName("keeps a plain http backend on ws, for local development")
	void keepsHttpOnWs() {
		assertThat(RawWorldSocket.socketUrl("http://localhost:3000", ""))
				.isEqualTo("ws://localhost:3000/v1/world/socket?instance=0");
	}

	@Test
	@DisplayName("tolerates a trailing slash rather than producing a doubled path")
	void trimsATrailingSlash() {
		assertThat(RawWorldSocket.socketUrl("https://echoes.example.com/", ""))
				.isEqualTo("wss://echoes.example.com/v1/world/socket?instance=0");
	}

	@Test
	@DisplayName("prefers an explicit socket override, which is how the local bridge is reached")
	void prefersTheOverride() {
		// The dev bridge cannot share Next's port, so the socket endpoint has to
		// be addressable independently of the HTTP backend.
		assertThat(RawWorldSocket.socketUrl("http://localhost:3000", "ws://localhost:3001"))
				.isEqualTo("ws://localhost:3001/v1/world/socket?instance=0");
	}

	@Test
	@DisplayName("normalises an override given as an http URL")
	void normalisesAnHttpOverride() {
		assertThat(RawWorldSocket.socketUrl("https://echoes.example.com", "http://localhost:3001/"))
				.isEqualTo("ws://localhost:3001/v1/world/socket?instance=0");
	}
}
