/*
 * I am the Dungeon Boss
 * Copyright (C) 2026 Dungeon Boss
 */

package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Guards release.ps1: after a successful GitHub Release, iatdb only invokes
 * hero-echoes {@code scripts/promote-vercel.ps1} (Vercel ownership lives
 * there).
 */
class ReleaseVercelPromoteWiringTest {

	@Test
	@DisplayName("release.ps1 promotes hero-echoes Vercel production after gh release create")
	void releasePromotesVercelAfterGitHubRelease() throws IOException {
		String source = readSource("scripts/release.ps1");
		int releaseCreate = source.indexOf("'release', 'create'");
		// Call site after gh release create (not the earlier dot-source of the .ps1).
		int promoteCall = source.indexOf("Publish-HeroEchoesVercelProduction `", releaseCreate);

		Assertions.assertThat(releaseCreate).isGreaterThanOrEqualTo(0);
		Assertions.assertThat(promoteCall).isGreaterThan(releaseCreate);
	}

	@Test
	@DisplayName("Publish-HeroEchoesVercelProduction delegates to hero-echoes promote-vercel.ps1")
	void promoteScriptDelegatesToHeroEchoesRepo() throws IOException {
		String source = readSource("scripts/release/Publish-HeroEchoesVercelProduction.ps1");

		Assertions.assertThat(source).contains("function Publish-HeroEchoesVercelProduction");
		Assertions.assertThat(source).contains("promote-vercel.ps1");
		Assertions.assertThat(source).doesNotContain("githubCommitSha=");
	}

	private static String readSource(String relativePath) throws IOException {
		Path file = findRepoFile(relativePath);
		return Files.readString(file, StandardCharsets.UTF_8);
	}

	private static Path findRepoFile(String relativePath) {
		Path dir = Paths.get("").toAbsolutePath();
		for (int i = 0; i < 8 && dir != null; i++) {
			Path candidate = dir.resolve(relativePath);
			if (Files.isRegularFile(dir.resolve("settings.gradle")) && Files.isRegularFile(candidate)) {
				return candidate;
			}
			dir = dir.getParent();
		}
		throw new AssertionError("Could not find " + relativePath + " from " + Paths.get("").toAbsolutePath());
	}
}
