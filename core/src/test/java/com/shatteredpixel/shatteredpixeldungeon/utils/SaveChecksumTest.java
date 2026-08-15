package com.shatteredpixel.shatteredpixeldungeon.utils;

import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

/**
 * The trailer is deliberately unmarked, so these cases pin the two things that
 * make it work at all: that a signed file is never mistaken for an unsigned one,
 * and that no edit to either half reads back as clean.
 */
@ExtendWith(GdxTestExtension.class)
class SaveChecksumTest {

	private static final String FILE = "checksum-test.dat";

	private static final long SEED = 0x0123456789ABCDEFL;
	private static final int DEPTH = 5;
	private static final int BRANCH = 0;

	@AfterEach
	void tearDown() {
		FileUtils.deleteFile(FILE);
		FileUtils.deleteFile(FILE + ".spdtmp");
	}

	private static Bundle sampleBundle() {
		Bundle bundle = new Bundle();
		bundle.put("gold", 1234);
		bundle.put("name", "hero");
		return bundle;
	}

	private static byte[] rawBytes() {
		return FileUtils.getFileHandle(FILE).readBytes();
	}

	private static void writeRaw(byte[] bytes) {
		FileHandle handle = FileUtils.getFileHandle(FILE);
		handle.writeBytes(bytes, false);
	}

	@Test
	@DisplayName("a signed file round-trips unchanged and verifies clean")
	void signedFileRoundTrips() throws IOException {
		SaveChecksum.write(FILE, sampleBundle(), SEED, DEPTH, BRANCH, false);

		SaveChecksum.Result result = SaveChecksum.read(FILE, SEED, DEPTH, BRANCH);

		Assertions.assertThat(result.state).isEqualTo(SaveChecksum.State.OK);
		Assertions.assertThat(result.bundle.getInt("gold")).isEqualTo(1234);
		Assertions.assertThat(result.bundle.getString("name")).isEqualTo("hero");
	}

	@Test
	@DisplayName("a file corrupted mid-stream is a read failure, not a verdict")
	void corruptedPayloadStillFailsToRead() throws IOException {
		SaveChecksum.write(FILE, sampleBundle(), SEED, DEPTH, BRANCH, false);

		// flipping a bit inside the compressed stream breaks its checksum, so neither the
		// signed nor the legacy reading of the file parses. That is ordinary save
		// corruption and keeps the base game's behaviour rather than becoming a detection.
		byte[] bytes = rawBytes();
		bytes[bytes.length - 20] ^= 0x01;
		writeRaw(bytes);

		Assertions.assertThatExceptionOfType(IOException.class)
				.isThrownBy(() -> SaveChecksum.read(FILE, SEED, DEPTH, BRANCH));
	}

	@Test
	@DisplayName("a save editor that rewrites the payload as plain JSON is detected")
	void rewrittenPayloadIsTampered() throws IOException {
		SaveChecksum.write(FILE, sampleBundle(), SEED, DEPTH, BRANCH, false);
		byte[] signed = rawBytes();

		// what a save editor produces: a valid bundle, but not the bytes we signed
		Bundle edited = sampleBundle();
		edited.put("gold", 999999);
		SaveChecksum.write(FILE, edited, SEED, DEPTH, BRANCH, false);
		byte[] editedPayload = rawBytes();

		// keep the original trailer, swap in the edited payload
		byte[] forged = new byte[editedPayload.length];
		System.arraycopy(editedPayload, 0, forged, 0, editedPayload.length - 8);
		System.arraycopy(signed, signed.length - 8, forged, forged.length - 8, 8);
		writeRaw(forged);

		Assertions.assertThat(SaveChecksum.read(FILE, SEED, DEPTH, BRANCH).state)
				.isEqualTo(SaveChecksum.State.TAMPERED);
	}

	@Test
	@DisplayName("overwriting the trailer with an arbitrary value is detected")
	void editedTrailerIsTampered() throws IOException {
		SaveChecksum.write(FILE, sampleBundle(), SEED, DEPTH, BRANCH, false);

		byte[] bytes = rawBytes();
		for (int i = 1; i <= 8; i++) {
			bytes[bytes.length - i] = 0x42;
		}
		writeRaw(bytes);

		Assertions.assertThat(SaveChecksum.read(FILE, SEED, DEPTH, BRANCH).state)
				.isEqualTo(SaveChecksum.State.TAMPERED);
	}

	@Test
	@DisplayName("a file signed for another run does not verify here")
	void otherRunSeedIsTampered() throws IOException {
		SaveChecksum.write(FILE, sampleBundle(), SEED, DEPTH, BRANCH, false);

		Assertions.assertThat(SaveChecksum.read(FILE, SEED + 1, DEPTH, BRANCH).state)
				.isEqualTo(SaveChecksum.State.TAMPERED);
	}

	@Test
	@DisplayName("a file signed for another depth or branch does not verify here")
	void otherDepthOrBranchIsTampered() throws IOException {
		SaveChecksum.write(FILE, sampleBundle(), SEED, DEPTH, BRANCH, false);

		Assertions.assertThat(SaveChecksum.read(FILE, SEED, DEPTH + 1, BRANCH).state)
				.isEqualTo(SaveChecksum.State.TAMPERED);
		Assertions.assertThat(SaveChecksum.read(FILE, SEED, DEPTH, BRANCH + 1).state)
				.isEqualTo(SaveChecksum.State.TAMPERED);
	}

	@Test
	@DisplayName("a poisoned file reads back as already flagged, not as a new detection")
	void poisonedFileReadsAsFlagged() throws IOException {
		SaveChecksum.write(FILE, sampleBundle(), SEED, DEPTH, BRANCH, true);

		SaveChecksum.Result result = SaveChecksum.read(FILE, SEED, DEPTH, BRANCH);

		Assertions.assertThat(result.state).isEqualTo(SaveChecksum.State.FLAGGED);
		Assertions.assertThat(result.bundle.getInt("gold")).isEqualTo(1234);
	}

	@Test
	@DisplayName("an unsigned legacy file is recognised, not flagged")
	void legacyFileIsUnsigned() throws IOException {
		FileUtils.bundleToFile(FILE, sampleBundle());

		SaveChecksum.Result result = SaveChecksum.read(FILE, SEED, DEPTH, BRANCH);

		Assertions.assertThat(result.state).isEqualTo(SaveChecksum.State.UNSIGNED);
		Assertions.assertThat(result.bundle.getInt("gold")).isEqualTo(1234);
	}

	@Test
	@DisplayName("a signed file is never mistaken for a legacy one")
	void signedFileIsNeverReadAsLegacy() throws IOException {
		// the discriminator is "does the file minus its trailer still parse"; run it
		// across many payload sizes so a lucky byte alignment cannot hide a mix-up
		for (int size = 0; size < 40; size++) {
			Bundle bundle = new Bundle();
			StringBuilder padding = new StringBuilder();
			for (int i = 0; i < size; i++) {
				padding.append('x');
			}
			bundle.put("pad", padding.toString());

			SaveChecksum.write(FILE, bundle, SEED, DEPTH, BRANCH, false);

			Assertions.assertThat(SaveChecksum.read(FILE, SEED, DEPTH, BRANCH).state)
					.as("payload padding size %d", size)
					.isEqualTo(SaveChecksum.State.OK);
		}
	}

	@Test
	@DisplayName("signing is stable across writes of identical content")
	void signingIsDeterministic() throws IOException {
		SaveChecksum.write(FILE, sampleBundle(), SEED, DEPTH, BRANCH, false);
		byte[] first = rawBytes();

		SaveChecksum.write(FILE, sampleBundle(), SEED, DEPTH, BRANCH, false);
		byte[] second = rawBytes();

		Assertions.assertThat(second).isEqualTo(first);
	}
}
