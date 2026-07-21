package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoOnlineSettings;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code project-links.properties} is the single source of truth.
 * {@link ProjectLinks} loads it; docs use markdown reference links whose
 * definitions must match (see {@code docs/project-link-refs.md}).
 */
class ProjectLinksTest {

	private static final String PROPERTIES_PATH = "services/src/main/resources/project-links.properties";
	private static final String REFS_PATH = "docs/project-link-refs.md";

	private static final String[] DOCS_WITH_HOMEPAGE = {
			"README.md",
			"docs/PENDING-LINKS.md",
	};

	private static final String[] DOCS_WITH_GITHUB_REPO = {
			"README.md",
			"docs/PENDING-LINKS.md",
			"docs/getting-started-desktop.md",
			"docs/getting-started-android.md",
			"docs/getting-started-ios.md",
	};

	private static final String[] DOCS_WITH_GITHUB_RELEASES = {
			"README.md",
			"docs/PENDING-LINKS.md",
			"docs/release/prepare-release.md",
	};

	private static final String[] DOCS_WITH_DEVELOPER_EMAIL = {
			"docs/PENDING-LINKS.md",
			"docs/getting-started-android.md",
			"docs/release/01-coming-soon.md",
	};

	@Test
	@DisplayName("classpath resource project-links.properties is present")
	void classpathResourceIsPresent() {
		try (InputStream in = ProjectLinks.class.getResourceAsStream("/" + ProjectLinks.RESOURCE_NAME)) {
			Assertions.assertThat(in)
					.as("expected /%s on classpath", ProjectLinks.RESOURCE_NAME)
					.isNotNull();
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	@Test
	@DisplayName("ProjectLinks values match project-links.properties on disk")
	void valuesMatchPropertiesFile() throws IOException {
		Properties props = loadDiskProperties();

		Assertions.assertThat(ProjectLinks.HOMEPAGE_URL)
				.isEqualTo(required(props, "homepage.url"));
		Assertions.assertThat(ProjectLinks.BACKEND_URL)
				.isEqualTo(required(props, "backend.url"));
		Assertions.assertThat(ProjectLinks.GITHUB_OWNER_REPO)
				.isEqualTo(required(props, "github.owner.repo"));
		Assertions.assertThat(ProjectLinks.DEVELOPER_EMAIL)
				.isEqualTo(required(props, "developer.email"));

		Assertions.assertThat(ProjectLinks.GITHUB_REPO_URL)
				.isEqualTo("https://github.com/" + ProjectLinks.GITHUB_OWNER_REPO);
		Assertions.assertThat(ProjectLinks.GITHUB_RELEASES_URL)
				.isEqualTo(ProjectLinks.GITHUB_REPO_URL + "/releases");
		Assertions.assertThat(ProjectLinks.GITHUB_RELEASES_API_URL)
				.isEqualTo("https://api.github.com/repos/" + ProjectLinks.GITHUB_OWNER_REPO + "/releases");
		Assertions.assertThat(ProjectLinks.HOMEPAGE_HOST)
				.isEqualTo(stripScheme(ProjectLinks.HOMEPAGE_URL));
		Assertions.assertThat(ProjectLinks.DEVELOPER_EMAIL_MAILTO)
				.isEqualTo("mailto:" + ProjectLinks.DEVELOPER_EMAIL);
	}

	@Test
	@DisplayName("project-link-refs.md definitions match ProjectLinks")
	void projectLinkRefsMatchProjectLinks() throws IOException {
		String refs = readSource(REFS_PATH);
		assertMarkdownRef(refs, "homepage", ProjectLinks.HOMEPAGE_URL);
		assertMarkdownRef(refs, "github-repo", ProjectLinks.GITHUB_REPO_URL);
		assertMarkdownRef(refs, "github-releases", ProjectLinks.GITHUB_RELEASES_URL);
		assertMarkdownRef(refs, "developer-email", ProjectLinks.DEVELOPER_EMAIL_MAILTO);
	}

	@Test
	@DisplayName("public docs define project link refs matching ProjectLinks")
	void publicDocsDefineMatchingProjectLinkRefs() throws IOException {
		for (String doc : DOCS_WITH_HOMEPAGE) {
			assertMarkdownRef(readSource(doc), "homepage", ProjectLinks.HOMEPAGE_URL);
		}
		for (String doc : DOCS_WITH_GITHUB_REPO) {
			assertMarkdownRef(readSource(doc), "github-repo", ProjectLinks.GITHUB_REPO_URL);
		}
		for (String doc : DOCS_WITH_GITHUB_RELEASES) {
			assertMarkdownRef(readSource(doc), "github-releases", ProjectLinks.GITHUB_RELEASES_URL);
		}
		for (String doc : DOCS_WITH_DEVELOPER_EMAIL) {
			assertMarkdownRef(readSource(doc), "developer-email", ProjectLinks.DEVELOPER_EMAIL_MAILTO);
		}
	}

	@Test
	@DisplayName("public docs use reference links instead of inline project URLs")
	void publicDocsUseReferenceLinksNotInlineProjectUrls() throws IOException {
		String[] allDocs = distinct(
				DOCS_WITH_HOMEPAGE,
				DOCS_WITH_GITHUB_REPO,
				DOCS_WITH_GITHUB_RELEASES,
				DOCS_WITH_DEVELOPER_EMAIL);
		for (String doc : allDocs) {
			String text = readSource(doc);
			Assertions.assertThat(text)
					.as("%s must not inline homepage URL", doc)
					.doesNotContain("](" + ProjectLinks.HOMEPAGE_URL + ")");
			Assertions.assertThat(text)
					.as("%s must not inline github-repo URL", doc)
					.doesNotContain("](" + ProjectLinks.GITHUB_REPO_URL + ")");
			Assertions.assertThat(text)
					.as("%s must not inline github-releases URL", doc)
					.doesNotContain("](" + ProjectLinks.GITHUB_RELEASES_URL + ")");
			Assertions.assertThat(text)
					.as("%s must not inline mailto developer email", doc)
					.doesNotContain("](" + ProjectLinks.DEVELOPER_EMAIL_MAILTO + ")");
		}
		Assertions.assertThat(readSource("README.md")).contains("[github-releases]");
		Assertions.assertThat(readSource("README.md")).contains("[homepage]");
		Assertions.assertThat(readSource("README.md")).contains("[github-repo]");
		Assertions.assertThat(readSource("docs/getting-started-desktop.md")).contains("[github-repo]");
		Assertions.assertThat(readSource("docs/release/prepare-release.md")).contains("[github-releases]");
		String emailLink = "[" + ProjectLinks.DEVELOPER_EMAIL + "][developer-email]";
		for (String doc : DOCS_WITH_DEVELOPER_EMAIL) {
			Assertions.assertThat(readSource(doc))
					.as("%s should link the contact email via developer-email ref", doc)
					.contains(emailLink);
		}
	}

	@Test
	@DisplayName("EchoOnlineSettings production backend reuses ProjectLinks")
	void echoOnlineSettingsReusesProjectLinksBackend() {
		Assertions.assertThat(EchoOnlineSettings.PRODUCTION_BACKEND_URL)
				.isEqualTo(ProjectLinks.BACKEND_URL);
	}

	@Test
	@DisplayName("PENDING-LINKS names project-links.properties as the source of truth")
	void pendingLinksNamesPropertiesAsSourceOfTruth() throws IOException {
		String pending = readSource("docs/PENDING-LINKS.md");
		Assertions.assertThat(pending).contains("project-links.properties");
		Assertions.assertThat(pending).contains("project-link-refs.md");
	}

	@Test
	@DisplayName("release.ps1 loads GitHub and email from project-links.properties")
	void releaseScriptLoadsProjectLinks() throws IOException {
		String source = readSource("scripts/release.ps1");
		Assertions.assertThat(source).contains("project-links.properties");
		Assertions.assertThat(source).contains("github.owner.repo");
		Assertions.assertThat(source).contains("developer.email");
		Assertions.assertThat(source).doesNotContain(ProjectLinks.GITHUB_OWNER_REPO);
		Assertions.assertThat(source).doesNotContain(ProjectLinks.DEVELOPER_EMAIL);
	}

	@Test
	@DisplayName("NewsScene read-more opens ProjectLinks homepage")
	void newsSceneUsesProjectHomepage() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/NewsScene.java");
		Assertions.assertThat(source).contains("ProjectLinks.HOMEPAGE_URL");
		Assertions.assertThat(source).doesNotContain("openURI(\"https://ShatteredPixel.com\")");
	}

	@Test
	@DisplayName("AboutScene IATDB block links to ProjectLinks homepage")
	void aboutSceneIatdbUsesProjectHomepage() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/AboutScene.java");
		Assertions.assertThat(source).contains("ProjectLinks.HOMEPAGE_URL");
		Assertions.assertThat(source).contains("ProjectLinks.HOMEPAGE_HOST");
	}

	@Test
	@DisplayName("GitHubUpdates checks IATDB releases via ProjectLinks")
	void githubUpdatesUsesProjectReleasesApi() throws IOException {
		String source = readSource(
				"services/updates/githubUpdates/src/main/java/com/shatteredpixel/shatteredpixeldungeon/services/updates/GitHubUpdates.java");
		Assertions.assertThat(source).contains("ProjectLinks.GITHUB_RELEASES_API_URL");
		Assertions.assertThat(source).doesNotContain("00-Evan/shattered-pixel-dungeon");
	}

	@Test
	@DisplayName("desktop crash dialogs use ProjectLinks developer email")
	void desktopLauncherUsesProjectEmail() throws IOException {
		String source = readSource(
				"desktop/src/main/java/com/shatteredpixel/shatteredpixeldungeon/desktop/DesktopLauncher.java");
		Assertions.assertThat(source).contains("ProjectLinks.DEVELOPER_EMAIL");
		Assertions.assertThat(source).doesNotContain(ProjectLinks.DEVELOPER_EMAIL);
	}

	@Test
	@DisplayName("Android missing-natives dialog uses ProjectLinks developer email")
	void androidMissingNativesUsesProjectEmail() throws IOException {
		String source = readSource(
				"android/src/main/java/com/shatteredpixel/shatteredpixeldungeon/android/AndroidMissingNativesHandler.java");
		Assertions.assertThat(source).contains("ProjectLinks.DEVELOPER_EMAIL");
		Assertions.assertThat(source).doesNotContain(ProjectLinks.DEVELOPER_EMAIL);
	}

	private static void assertMarkdownRef(String markdown, String label, String expectedUrl) {
		Pattern pattern = Pattern.compile("^\\[" + Pattern.quote(label) + "\\]:\\s*(\\S+)\\s*$", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(markdown);
		Assertions.assertThat(matcher.find())
				.as("missing markdown ref [%s]:", label)
				.isTrue();
		Assertions.assertThat(matcher.group(1)).isEqualTo(expectedUrl);
	}

	private static String[] distinct(String[]... groups) {
		java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
		for (String[] group : groups) {
			java.util.Collections.addAll(set, group);
		}
		return set.toArray(new String[0]);
	}

	private static Properties loadDiskProperties() throws IOException {
		Path file = findRepoFile(PROPERTIES_PATH);
		Properties props = new Properties();
		try (InputStream in = Files.newInputStream(file)) {
			props.load(in);
		}
		return props;
	}

	private static String required(Properties props, String key) {
		String value = props.getProperty(key);
		if (value == null || value.trim().isEmpty()) {
			throw new AssertionError("missing property: " + key);
		}
		return value.trim();
	}

	private static String stripScheme(String url) {
		return url.replaceFirst("^https?://", "");
	}

	private static String readSource(String relativePath) throws IOException {
		return Files.readString(findRepoFile(relativePath), StandardCharsets.UTF_8);
	}

	private static Path findRepoFile(String relativePath) {
		Path dir = Paths.get("").toAbsolutePath();
		for (int i = 0; i < 8 && dir != null; i++) {
			Path candidate = dir.resolve(relativePath);
			if (Files.isRegularFile(candidate)) {
				return candidate;
			}
			dir = dir.getParent();
		}
		throw new AssertionError("Could not find " + relativePath + " from " + Paths.get("").toAbsolutePath());
	}
}
