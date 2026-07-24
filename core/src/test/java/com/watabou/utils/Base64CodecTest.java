package com.watabou.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

class Base64CodecTest {

	@Test
	@DisplayName("encode and decode round-trip binary payload")
	void encodeDecodeRoundTrip() {
		byte[] original = "gzip-echo-blob\0\1\2".getBytes(StandardCharsets.UTF_8);
		String encoded = Base64Codec.encode(original);
		Assertions.assertThat(Base64Codec.decode(encoded)).isEqualTo(original);
	}

	@Test
	@DisplayName("matches java.util.Base64 basic encoder (same wire/DB strings)")
	void matchesJavaUtilBase64Basic() {
		byte[] original = new byte[256];
		for (int i = 0; i < original.length; i++) {
			original[i] = (byte) i;
		}
		Assertions.assertThat(Base64Codec.encode(original))
				.isEqualTo(java.util.Base64.getEncoder().encodeToString(original));
		String wired = java.util.Base64.getEncoder().encodeToString(original);
		Assertions.assertThat(Base64Codec.decode(wired)).isEqualTo(original);
	}

	@Test
	@DisplayName("rejects illegal base64 input")
	void rejectsIllegalInput() {
		Assertions.assertThatThrownBy(() -> Base64Codec.decode("@@@"))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
