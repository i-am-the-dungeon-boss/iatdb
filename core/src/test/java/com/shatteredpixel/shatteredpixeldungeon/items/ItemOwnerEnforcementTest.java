package com.shatteredpixel.shatteredpixeldungeon.items;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Equipment describes itself against its owner, not against the player.
 *
 * <p>Who is <em>reading</em> a description is always the local player and never
 * changes what it should say. Whose sword it is does. Description code therefore
 * reads {@code owner()} and {@code isEquippedByOwner()} rather than
 * {@code Dungeon.hero} — which for an item in play is the same hero, and for a
 * preview of an echo's kit is the echo. That is a rule about a whole family of
 * methods across three dozen base-game classes, including ones that arrive by
 * merging upstream, so it is checked here rather than remembered.
 *
 * <p>Gameplay code is untouched by this and still reads {@code Dungeon.hero};
 * only the bodies of the description methods below are scanned.
 */
class ItemOwnerEnforcementTest {

	/** Worn or wielded: the slots an echo inspect actually renders. */
	private static final String[] EQUIPMENT_ROOTS = {
			"items/weapon",
			"items/armor",
			"items/artifacts",
			"items/rings"
	};

	private static final String[] EQUIPMENT_FILES = {
			"items/BrokenSeal.java",
			"items/KindofMisc.java"
	};

	private static final String PACKAGE_ROOT =
			"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon";

	/** The methods whose output is description text shown to a viewer. */
	private static final Pattern DESCRIPTION_METHOD = Pattern.compile(
			"\\b(info|desc|statsInfo|abilityInfo|typicalAbilityInfo|upgradeAbilityStat)"
					+ "\\s*\\([^;{()]*\\)\\s*\\{");

	@Test
	@DisplayName("equipment descriptions read the owner, never Dungeon.hero")
	void equipmentDescriptionsReadTheOwner() throws IOException {
		Path root = findRepoDir(PACKAGE_ROOT);
		Assertions.assertThat(root).as("package root not found").isNotNull();

		List<String> offenders = new ArrayList<>();
		for (String dir : EQUIPMENT_ROOTS) {
			collect(root.resolve(dir), offenders);
		}
		for (String file : EQUIPMENT_FILES) {
			scan(root.resolve(file), offenders);
		}

		Assertions.assertThat(offenders)
				.as("use owner() / isEquippedByOwner() in equipment description methods,"
						+ " so an echo's kit is described against the echo that owns it")
				.isEmpty();
	}

	private static void collect(Path dir, List<String> offenders) throws IOException {
		if (!Files.isDirectory(dir)) {
			return;
		}
		List<Path> sources = new ArrayList<>();
		gather(dir, sources);
		for (int i = 0; i < sources.size(); i++) {
			scan(sources.get(i), offenders);
		}
	}

	private static void gather(Path dir, List<Path> into) throws IOException {
		for (Path child : Files.newDirectoryStream(dir)) {
			if (Files.isDirectory(child)) {
				gather(child, into);
			} else if (child.toString().endsWith(".java")) {
				into.add(child);
			}
		}
	}

	private static void scan(Path file, List<String> offenders) throws IOException {
		if (!Files.isRegularFile(file)) {
			return;
		}
		String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
		if (!source.contains("Dungeon.hero")) {
			return;
		}
		String name = file.getFileName().toString();
		Matcher method = DESCRIPTION_METHOD.matcher(source);
		while (method.find()) {
			int open = source.indexOf('{', method.end() - 1);
			int close = closingBrace(source, open);
			String body = source.substring(open, close);
			if (body.contains("Dungeon.hero")) {
				offenders.add(name + "." + method.group(1) + "()");
			}
		}
	}

	private static int closingBrace(String source, int open) {
		int depth = 0;
		for (int i = open; i < source.length(); i++) {
			char c = source.charAt(i);
			if (c == '{') {
				depth++;
			} else if (c == '}') {
				depth--;
				if (depth == 0) {
					return i;
				}
			}
		}
		return source.length();
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
