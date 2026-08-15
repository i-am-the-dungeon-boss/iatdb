package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;

import com.watabou.utils.FileUtils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The game's save sandbox is libGDX's {@code Local} type, which resolves against
 * the process working directory. A headless run whose working directory sits in
 * the source tree therefore drops real save data ({@code bones.dat},
 * {@code game<n>-solo/}, {@code echoes-solo/}) among committed sources, where it
 * gets picked up as files to commit.
 */
@ExtendWith(GdxTestExtension.class)
class GameFileSandboxTest {

	@Test
	@DisplayName("game files are written outside the source tree")
	void gameFilesAreWrittenOutsideTheSourceTree() {
		String path = FileUtils.getFileHandle("bones.dat").file().getAbsolutePath()
				.replace('\\', '/');

		Assertions.assertThat(path).doesNotContain("/src/main/").doesNotContain("/src/test/");
	}
}
