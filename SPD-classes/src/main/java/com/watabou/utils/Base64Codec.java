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

package com.watabou.utils;

/**
 * Standard (RFC 4648) Base64 encode/decode. Use instead of the JDK Base64
 * class, which RoboVM's runtime does not provide. Output matches the basic
 * encoder — same strings as existing {@code echo_data_base64} wire/DB values
 * (no migration).
 */
public final class Base64Codec {

	private static final char[] ENCODE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
			.toCharArray();
	private static final int[] DECODE = new int[256];

	static {
		for (int i = 0; i < DECODE.length; i++) {
			DECODE[i] = -1;
		}
		for (int i = 0; i < ENCODE.length; i++) {
			DECODE[ENCODE[i]] = i;
		}
		DECODE['='] = -2;
	}

	private Base64Codec() {
	}

	public static String encode(byte[] data) {
		if (data == null || data.length == 0) {
			return "";
		}
		int outLen = 4 * ((data.length + 2) / 3);
		StringBuilder sb = new StringBuilder(outLen);
		int i = 0;
		while (i + 2 < data.length) {
			int n = ((data[i] & 0xff) << 16) | ((data[i + 1] & 0xff) << 8) | (data[i + 2] & 0xff);
			sb.append(ENCODE[(n >>> 18) & 63]);
			sb.append(ENCODE[(n >>> 12) & 63]);
			sb.append(ENCODE[(n >>> 6) & 63]);
			sb.append(ENCODE[n & 63]);
			i += 3;
		}
		if (i < data.length) {
			int b0 = data[i] & 0xff;
			sb.append(ENCODE[b0 >>> 2]);
			if (i + 1 < data.length) {
				int b1 = data[i + 1] & 0xff;
				sb.append(ENCODE[((b0 & 3) << 4) | (b1 >>> 4)]);
				sb.append(ENCODE[(b1 & 15) << 2]);
				sb.append('=');
			} else {
				sb.append(ENCODE[(b0 & 3) << 4]);
				sb.append('=');
				sb.append('=');
			}
		}
		return sb.toString();
	}

	public static byte[] decode(String encoded) {
		if (encoded == null || encoded.isEmpty()) {
			return new byte[0];
		}
		// Strip whitespace the way many transports insert it; reject other junk.
		StringBuilder cleaned = new StringBuilder(encoded.length());
		for (int i = 0; i < encoded.length(); i++) {
			char c = encoded.charAt(i);
			if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
				continue;
			}
			cleaned.append(c);
		}
		int len = cleaned.length();
		if ((len & 3) != 0) {
			throw new IllegalArgumentException("invalid base64 length");
		}
		int pad = 0;
		if (len >= 1 && cleaned.charAt(len - 1) == '=') {
			pad++;
		}
		if (len >= 2 && cleaned.charAt(len - 2) == '=') {
			pad++;
		}
		byte[] out = new byte[(len / 4) * 3 - pad];
		int outPos = 0;
		for (int i = 0; i < len; i += 4) {
			int c0 = decodeChar(cleaned.charAt(i));
			int c1 = decodeChar(cleaned.charAt(i + 1));
			int c2 = decodeChar(cleaned.charAt(i + 2));
			int c3 = decodeChar(cleaned.charAt(i + 3));
			if (c0 < 0 || c1 < 0 || (c2 < 0 && c2 != -2) || (c3 < 0 && c3 != -2)) {
				throw new IllegalArgumentException("invalid base64 char");
			}
			int n = (c0 << 18) | (c1 << 12);
			out[outPos++] = (byte) (n >>> 16);
			if (c2 != -2) {
				n |= c2 << 6;
				out[outPos++] = (byte) ((n >>> 8) & 0xff);
				if (c3 != -2) {
					n |= c3;
					out[outPos++] = (byte) (n & 0xff);
				}
			}
		}
		return out;
	}

	private static int decodeChar(char c) {
		if (c > 255) {
			return -1;
		}
		return DECODE[c];
	}
}
