package com.watabou.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Guards production sources against JDK APIs missing from RoboVM's runtime
 * ({@code robovm-rt}), which is far older than the Java 11 compile target.
 */
class RoboVMUnsupportedApisTest {

	private static final Pattern LIST_SORT_LAMBDA_OR_COMPARATOR = Pattern.compile(
			"\\.sort\\s*\\(\\s*(\\(|Comparator\\.)");

	private static final String[] ROOTS = {
			"core/src/main/java",
			"SPD-classes/src/main/java",
			"desktop/src/main/java",
			"android/src/main/java",
			"ios/src/main/java"
	};

	@Test
	@DisplayName("production sources avoid JDK APIs missing from RoboVM runtime")
	void productionSourcesAvoidUnsupportedJdkApis() throws IOException {
		Map<String, List<String>> offenders = new LinkedHashMap<>();
		for (String root : ROOTS) {
			Path dir = findRepoDir(root);
			if (dir == null) {
				continue;
			}
			try (Stream<Path> paths = Files.walk(dir)) {
				paths.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
					try {
						String source = Files.readString(p, StandardCharsets.UTF_8);
						String rel = dir.relativize(p).toString().replace('\\', '/');
						check(source, "import java.util.function", offenders, rel);
						check(source, "java.util.Optional", offenders, rel);
						check(source, "java.util.Base64", offenders, rel);
						check(source, "List.of(", offenders, rel);
						check(source, "Map.of(", offenders, rel);
						check(source, "Set.of(", offenders, rel);
						check(source, ".removeIf(", offenders, rel);
						check(source, "String.join(", offenders, rel);
						check(source, "Comparator.comparing", offenders, rel);
						check(source, ".getOrDefault(", offenders, rel);
						check(source, "Integer::sum", offenders, rel);
						check(source, ".isBlank()", offenders, rel);
						if (LIST_SORT_LAMBDA_OR_COMPARATOR.matcher(source).find()) {
							List<String> list = offenders.get("List.sort(lambda/Comparator.*)");
							if (list == null) {
								list = new ArrayList<>();
								offenders.put("List.sort(lambda/Comparator.*)", list);
							}
							list.add(rel);
						}
					} catch (IOException e) {
						throw new AssertionError("failed reading " + p, e);
					}
				});
			}
		}
		Assertions.assertThat(offenders)
				.as("avoid/rewrite RoboVM-missing JDK APIs (see .cursor/rules/cross-platform.mdc)")
				.isEmpty();
	}

	private static void check(String source, String needle, Map<String, List<String>> offenders, String rel) {
		if (source.contains(needle)) {
			List<String> list = offenders.get(needle);
			if (list == null) {
				list = new ArrayList<>();
				offenders.put(needle, list);
			}
			list.add(rel);
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
