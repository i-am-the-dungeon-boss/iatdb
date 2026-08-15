package com.shatteredpixel.shatteredpixeldungeon.utils;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Writes save files with a keyed checksum of their own bytes appended after the
 * gzip stream, and reports on read whether those bytes are still the ones we wrote.
 * <p>
 * The checksum covers the whole payload rather than a list of fields, so it needs
 * no maintenance as the save format grows, and it cannot drift between the write
 * and read paths. It carries no marker of any kind: gzip readers stop at the end
 * of the stream, so the trailing bytes are invisible to anything that simply
 * decompresses the file. A file whose payload-minus-trailer no longer parses is an
 * older save written before signing existed; see {@link State#UNSIGNED}.
 */
public final class SaveChecksum {

	private static final int TRAILER_BYTES = 8;

	// Split so neither half appears as a single constant. Shipping in the client makes
	// this obfuscation, not secrecy — see the notes on Dungeon's save handling.
	private static final long KEY_HI = 0x9E3779B97F4A7C15L ^ 0x243F6A8885A308D3L;
	private static final long KEY_LO = 0xBF58476D1CE4E5B9L ^ 0x13198A2E03707344L;

	private static final long POISON = 0xA5A5C3C396966969L;

	// SipHash-2-4: two rounds per message block, four to finalize
	private static final int COMPRESSION_ROUNDS = 2;
	private static final int FINALIZATION_ROUNDS = 4;

	public enum State {
		/** Bytes match what we wrote. */
		OK,
		/** Bytes match what we wrote for a file already known to be modified. */
		FLAGGED,
		/** Bytes do not match; this is a new detection. */
		TAMPERED,
		/** Written before signing existed. Adopted on the next write, never a detection. */
		UNSIGNED
	}

	public static final class Result {

		public final Bundle bundle;
		public final State state;

		Result(Bundle bundle, State state) {
			this.bundle = bundle;
			this.state = state;
		}
	}

	private SaveChecksum() {
	}

	public static void write(String fileName, Bundle bundle, long seed, int depth, int branch,
			boolean poisoned) throws IOException {

		ByteArrayOutputStream payloadStream = new ByteArrayOutputStream();
		if (!Bundle.write(bundle, payloadStream)) {
			throw new IOException("could not serialize " + fileName);
		}
		byte[] payload = payloadStream.toByteArray();

		long tag = mac(payload, seed, depth, branch);
		if (poisoned) {
			tag ^= POISON;
		}

		byte[] out = new byte[payload.length + TRAILER_BYTES];
		System.arraycopy(payload, 0, out, 0, payload.length);
		putLong(out, payload.length, tag);

		writeBytes(fileName, out);
	}

	public static Result read(String fileName, long seed, int depth, int branch) throws IOException {

		byte[] bytes = readBytes(fileName);

		if (bytes.length > TRAILER_BYTES) {
			byte[] payload = new byte[bytes.length - TRAILER_BYTES];
			System.arraycopy(bytes, 0, payload, 0, payload.length);

			Bundle bundle = parseQuietly(payload);
			if (bundle != null) {
				long stored = getLong(bytes, payload.length);
				long expected = mac(payload, seed, depth, branch);

				if (stored == expected) {
					return new Result(bundle, State.OK);
				} else if (stored == (expected ^ POISON)) {
					return new Result(bundle, State.FLAGGED);
				} else {
					return new Result(bundle, State.TAMPERED);
				}
			}
		}

		// No parseable payload ahead of a trailer, so there is no trailer: an older save.
		// A genuine read failure here is a real fault and is reported as one.
		Bundle bundle = Bundle.read(new ByteArrayInputStream(bytes));
		return new Result(bundle, State.UNSIGNED);
	}

	private static Bundle parseQuietly(byte[] payload) {
		try {
			return Bundle.read(new ByteArrayInputStream(payload), false);
		} catch (IOException e) {
			return null;
		}
	}

	// file access, mirroring FileUtils' crash-safe temp-then-rename write

	private static byte[] readBytes(String fileName) throws IOException {
		try {
			FileHandle file = FileUtils.getFileHandle(fileName);
			if (!file.exists() || file.isDirectory() || file.length() == 0) {
				throw new IOException("file does not exist!");
			}
			return file.readBytes();
		} catch (GdxRuntimeException e) {
			throw new IOException(e);
		}
	}

	private static void writeBytes(String fileName, byte[] bytes) throws IOException {
		try {
			FileHandle file = FileUtils.getFileHandle(fileName);

			if (file.exists()) {
				FileHandle temp = FileUtils.getFileHandle(fileName + ".spdtmp");
				temp.writeBytes(bytes, false);
				file.delete();
				temp.moveTo(file);
			} else {
				file.writeBytes(bytes, false);
			}
		} catch (GdxRuntimeException e) {
			throw new IOException(e);
		}
	}

	// SipHash-2-4. Pure long arithmetic so it runs identically on desktop, Android and iOS.

	private static long mac(byte[] data, long seed, int depth, int branch) {

		// the run identity rides in the key, so a file signed for one run cannot be
		// dropped into another, and no copy of the payload is needed to mix it in
		long k0 = KEY_HI ^ seed;
		long k1 = KEY_LO ^ (((long) depth << 32) ^ (branch & 0xFFFFFFFFL));

		// one array for the whole file, not one per round: level files run to megabytes
		long[] v = {
				k0 ^ 0x736F6D6570736575L,
				k1 ^ 0x646F72616E646F6DL,
				k0 ^ 0x6C7967656E657261L,
				k1 ^ 0x7465646279746573L
		};

		int blocks = data.length / 8;
		for (int i = 0; i < blocks; i++) {
			long m = littleEndianLong(data, i * 8);
			v[3] ^= m;
			rounds(v, COMPRESSION_ROUNDS);
			v[0] ^= m;
		}

		long tail = ((long) data.length & 0xFF) << 56;
		int left = data.length & 7;
		int base = blocks * 8;
		for (int i = left - 1; i >= 0; i--) {
			tail |= (data[base + i] & 0xFFL) << (i * 8);
		}

		v[3] ^= tail;
		rounds(v, COMPRESSION_ROUNDS);
		v[0] ^= tail;

		v[2] ^= 0xFF;
		rounds(v, FINALIZATION_ROUNDS);

		return v[0] ^ v[1] ^ v[2] ^ v[3];
	}

	/** SIPROUND, applied in place. */
	private static void rounds(long[] v, int count) {
		long v0 = v[0];
		long v1 = v[1];
		long v2 = v[2];
		long v3 = v[3];

		for (int r = 0; r < count; r++) {
			v0 += v1;
			v1 = Long.rotateLeft(v1, 13);
			v1 ^= v0;
			v0 = Long.rotateLeft(v0, 32);
			v2 += v3;
			v3 = Long.rotateLeft(v3, 16);
			v3 ^= v2;
			v0 += v3;
			v3 = Long.rotateLeft(v3, 21);
			v3 ^= v0;
			v2 += v1;
			v1 = Long.rotateLeft(v1, 17);
			v1 ^= v2;
			v2 = Long.rotateLeft(v2, 32);
		}

		v[0] = v0;
		v[1] = v1;
		v[2] = v2;
		v[3] = v3;
	}

	private static long littleEndianLong(byte[] data, int offset) {
		long value = 0;
		for (int i = 7; i >= 0; i--) {
			value = (value << 8) | (data[offset + i] & 0xFFL);
		}
		return value;
	}

	private static void putLong(byte[] target, int offset, long value) {
		for (int i = 0; i < 8; i++) {
			target[offset + i] = (byte) (value >>> (56 - i * 8));
		}
	}

	private static long getLong(byte[] source, int offset) {
		long value = 0;
		for (int i = 0; i < 8; i++) {
			value = (value << 8) | (source[offset + i] & 0xFFL);
		}
		return value;
	}
}
