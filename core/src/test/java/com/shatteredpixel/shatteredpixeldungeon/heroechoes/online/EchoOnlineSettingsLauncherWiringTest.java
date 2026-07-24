package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.ProjectLinks;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Launchers live outside {@code :core}; this guards the dotenv wiring contract
 * by asserting the call sites remain in source.
 */
class EchoOnlineSettingsLauncherWiringTest {

	@Test
	@DisplayName("desktop launcher loads EchoOnlineSettings dotenv on startup")
	void desktopLauncherLoadsDotEnv() throws IOException {
		String source = readSource(
				"desktop/src/main/java/com/shatteredpixel/shatteredpixeldungeon/desktop/DesktopLauncher.java");

		Assertions.assertThat(source).contains("EchoOnlineSettings.loadDefaultDotEnv()");
	}

	@Test
	@DisplayName("android launcher loads EchoOnlineSettings dotenv on startup")
	void androidLauncherLoadsDotEnv() throws IOException {
		String source = readSource(
				"android/src/main/java/com/shatteredpixel/shatteredpixeldungeon/android/AndroidLauncher.java");

		Assertions.assertThat(source).contains("EchoOnlineSettings.loadDefaultDotEnv()");
	}

	@Test
	@DisplayName("android launcher applies BuildConfig echo defaults on startup")
	void androidLauncherAppliesBuildConfigDefaults() throws IOException {
		String source = readSource(
				"android/src/main/java/com/shatteredpixel/shatteredpixeldungeon/android/AndroidLauncher.java");

		Assertions.assertThat(source).contains("EchoOnlineSettings.setBuildDefaults");
		Assertions.assertThat(source).contains("BuildConfig.ECHO_BACKEND_URL");
		Assertions.assertThat(source).contains("BuildConfig.ECHO_API_KEY");
	}

	@Test
	@DisplayName("android build.gradle defines echo BuildConfig fields")
	void androidBuildGradleDefinesEchoBuildConfigFields() throws IOException {
		String source = readSource("android/build.gradle");

		Assertions.assertThat(source).contains("ECHO_BACKEND_URL");
		Assertions.assertThat(source).contains("ECHO_API_KEY");
		Assertions.assertThat(source).contains("buildConfig");
	}

	@Test
	@DisplayName("android build.gradle falls back to project-links.properties backend.url")
	void androidBuildGradleFallsBackToProjectLinksBackend() throws IOException {
		String source = readSource("android/build.gradle");

		Assertions.assertThat(source).contains("project-links.properties");
		Assertions.assertThat(source).contains("backend.url");
		Assertions.assertThat(source).doesNotContain(ProjectLinks.BACKEND_URL);
	}

	@Test
	@DisplayName("desktop launcher applies production backend URL when dotenv is empty")
	void desktopLauncherAppliesProductionBackendDefault() throws IOException {
		String source = readSource(
				"desktop/src/main/java/com/shatteredpixel/shatteredpixeldungeon/desktop/DesktopLauncher.java");

		Assertions.assertThat(source).contains("EchoOnlineSettings.PRODUCTION_BACKEND_URL");
		Assertions.assertThat(source).contains("EchoOnlineSettings.setBuildDefaults");
		Assertions.assertThat(source).doesNotContain("releaseApiKey");
		Assertions.assertThat(source).doesNotContain("ECHO_API_KEY_RELEASE");
	}

	@Test
	@DisplayName("ios launcher loads EchoOnlineSettings dotenv on startup")
	void iosLauncherLoadsDotEnv() throws IOException {
		String source = readSource(
				"ios/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ios/IOSLauncher.java");

		Assertions.assertThat(source).contains("EchoOnlineSettings.loadDefaultDotEnv()");
	}

	@Test
	@DisplayName("ios launcher applies production backend URL when dotenv is empty")
	void iosLauncherAppliesProductionBackendDefault() throws IOException {
		String source = readSource(
				"ios/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ios/IOSLauncher.java");

		Assertions.assertThat(source).contains("EchoOnlineSettings.PRODUCTION_BACKEND_URL");
		Assertions.assertThat(source).contains("EchoOnlineSettings.setBuildDefaults");
	}

	@Test
	@DisplayName("ios launcher sets EchoUpdates base URL override")
	void iosLauncherSetsEchoUpdatesBaseUrlOverride() throws IOException {
		String source = readSource(
				"ios/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ios/IOSLauncher.java");

		Assertions.assertThat(source).contains("EchoUpdates.baseUrlOverride");
		Assertions.assertThat(source).contains("EchoOnlineSettings.backendUrl()");
	}

	@Test
	@DisplayName("ios build.gradle uses echoUpdates and debugNews")
	void iosBuildGradleUsesEchoUpdatesAndDebugNews() throws IOException {
		String source = readSource("ios/build.gradle");

		Assertions.assertThat(source).contains("services:updates:echoUpdates");
		Assertions.assertThat(source).contains("services:news:debugNews");
		Assertions.assertThat(source).doesNotContain("services:updates:debugUpdates");
		Assertions.assertThat(source).doesNotContain("services:news:shatteredNews");
	}

	@Test
	@DisplayName("ios build.gradle main class matches IOSLauncher Java package")
	void iosBuildGradleMainClassMatchesJavaPackage() throws IOException {
		String source = readSource("ios/build.gradle");

		Assertions.assertThat(source)
				.contains("com.shatteredpixel.shatteredpixeldungeon.ios.IOSLauncher");
		Assertions.assertThat(source)
				.doesNotContain("appPackageName + \".ios.IOSLauncher\"");
	}

	@Test
	@DisplayName("ios launcher initializes Sentry with ios platform tag")
	void iosLauncherInitializesSentry() throws IOException {
		String source = readSource(
				"ios/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ios/IOSLauncher.java");

		Assertions.assertThat(source).contains("SentryCrashReporting.initForRelease");
		Assertions.assertThat(source).contains("\"ios\"");
		Assertions.assertThat(source).contains("SentryCrashReporting.reportAndFlush");
	}

	@Test
	@DisplayName("desktop launcher initializes Sentry via SentryCrashReporting")
	void desktopLauncherInitializesSentryViaHelper() throws IOException {
		String source = readSource(
				"desktop/src/main/java/com/shatteredpixel/shatteredpixeldungeon/desktop/DesktopLauncher.java");

		Assertions.assertThat(source).contains("SentryCrashReporting.initForRelease");
		Assertions.assertThat(source).contains("\"desktop\"");
		Assertions.assertThat(source).contains("SentryCrashReporting.reportAndFlush");
	}

	@Test
	@DisplayName("ios robovm.xml force-links Sentry classes")
	void iosRobovmForceLinksSentry() throws IOException {
		String source = readSource("ios/robovm.xml");

		Assertions.assertThat(source).contains("io.sentry.**");
	}

	@Test
	@DisplayName("production backend URL constant reuses ProjectLinks")
	void productionBackendUrlReusesProjectLinks() {
		Assertions.assertThat(EchoOnlineSettings.PRODUCTION_BACKEND_URL)
				.isEqualTo(ProjectLinks.BACKEND_URL);
	}

	@Test
	@DisplayName(".env.example does not hardcode production backend URL")
	void envExampleDoesNotHardcodeProductionBackend() throws IOException {
		String source = readSource(".env.example");

		Assertions.assertThat(source).contains("project-links.properties");
		Assertions.assertThat(source).doesNotContain(ProjectLinks.BACKEND_URL);
		Assertions.assertThat(source).doesNotContain("ECHO_BACKEND_URL_RELEASE=");
	}

	private static String readSource(String relativePath) throws IOException {
		Path file = findRepoFile(relativePath);
		return Files.readString(file, StandardCharsets.UTF_8);
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
