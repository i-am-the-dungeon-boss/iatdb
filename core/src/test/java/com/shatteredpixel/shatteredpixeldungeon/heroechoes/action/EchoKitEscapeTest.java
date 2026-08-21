package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The phantom kit is a {@code Hero}, so it is also a {@code Char}, and nothing
 * in the type system stops it being handed to world-facing code that draws on
 * {@code sprite}. That produced the same defect three times — ANDROID-21 in
 * {@code EchoClericHandlers.holyWeapon} and {@code holyWard}, and a third live
 * copy in {@code InventoryStoneEchoBridge.applyEnchantment}.
 *
 * <p>Making the field private behind {@code gear()} / {@code stats()} narrows
 * and labels the surface, but cannot make the wrong call a compile error:
 * {@code chargeUse(Hero)} and {@code Enchanting.show(Char, Item)} accept the
 * same reference indistinguishably. This scan closes the residual.
 */
class EchoKitEscapeTest {

	/** VFX entry points that render at their {@code Char} argument's sprite. */
	private static final Pattern WORLD_FX_ON_KIT = Pattern.compile(
			"(?:Enchanting\\.show|MagicMissile\\.boltFromChar|new\\s+Flare\\([^)]*\\)\\.show)"
					+ "\\s*\\(\\s*(?:[A-Za-z0-9_.]*\\.)?(?:kit|stats\\(\\))\\b");

	/** The kit's sprite is mirrored from its body; draw on the body instead. */
	private static final Pattern SPRITE_THROUGH_KIT = Pattern.compile(
			"\\b(?:ctx\\.)?(?:kit|stats\\(\\))\\.sprite\\b");

	@Test
	@DisplayName("echo code never hands the phantom kit to world-facing VFX")
	void echoCodeNeverHandsKitToWorldFx() throws IOException {
		Path root = findRepoDir("core/src/main/java");
		Assertions.assertThat(root).as("core sources must be locatable").isNotNull();

		Map<String, List<String>> offenders = new LinkedHashMap<>();
		try (Stream<Path> paths = Files.walk(root)) {
			paths.filter(EchoKitEscapeTest::isEchoSource).forEach(p -> {
				String rel = root.relativize(p).toString().replace('\\', '/');
				String source;
				try {
					source = Files.readString(p, StandardCharsets.UTF_8);
				} catch (IOException e) {
					throw new AssertionError("failed reading " + p, e);
				}
				record(WORLD_FX_ON_KIT, source, rel, "world VFX drawn on the kit", offenders);
				record(SPRITE_THROUGH_KIT, source, rel, "sprite reached through the kit", offenders);
			});
		}

		Assertions.assertThat(offenders)
				.as("pass the body to world VFX, not the kit "
						+ "(see .cursor/rules/echo-world-vfx.mdc)")
				.isEmpty();
	}

	/**
	 * Echo code lives under {@code heroechoes/} plus the {@code *EchoBridge}
	 * shims that sit beside the base-game classes they adapt.
	 */
	private static boolean isEchoSource(Path p) {
		String s = p.toString().replace('\\', '/');
		if (!s.endsWith(".java")) {
			return false;
		}
		if (s.endsWith("/EchoActionContext.java") || s.endsWith("/EchoKitBorrow.java")) {
			return false;
		}
		return s.contains("/heroechoes/") || s.endsWith("EchoBridge.java");
	}

	private static void record(
			Pattern pattern, String source, String rel, String label,
			Map<String, List<String>> offenders) {
		Matcher m = pattern.matcher(source);
		while (m.find()) {
			List<String> hits = offenders.get(label);
			if (hits == null) {
				hits = new ArrayList<>();
				offenders.put(label, hits);
			}
			hits.add(rel + " :: " + m.group());
		}
	}

	private static Path findRepoDir(String relativePath) {
		Path dir = Paths.get("").toAbsolutePath();
		for (int i = 0; i < 8 && dir != null; i++) {
			Path candidate = dir.resolve(relativePath);
			if (Files.isDirectory(candidate)) {
				return candidate;
			}
			dir = dir.getParent();
		}
		return null;
	}
}
