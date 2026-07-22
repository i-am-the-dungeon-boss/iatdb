package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoStorage;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoClient;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoClientTest;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoOnlineSettings;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoOnlineSync;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPlayerSession;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class WndGameEchoSaveTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("solo menu save writes canonical depth file with echo and policy")
	void soloMenuSaveWritesLoadableDepthFileWithPolicy() throws Exception {
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		Echo echo = EchoTestSupport.warriorEchoWithData(5);
		echo.echoId = "manual-test";

		WndGame.saveLocalEcho(echo);

		Assertions.assertThat(FileUtils.fileExists("echoes-solo/depth-5.dat")).isTrue();
		Bundle fileBundle = FileUtils.bundleFromFile("echoes-solo/depth-5.dat");
		Assertions.assertThat(fileBundle.contains(Echo.BUNDLE_KEY)).isTrue();
		Assertions.assertThat(fileBundle.contains(EchoStorage.POLICY_BUNDLE_KEY)).isTrue();
		Assertions.assertThat(new EchoStorage().findEchoForDepth(5).isFound()).isTrue();
	}

	@Test
	@DisplayName("ranked menu save uploads to backend and writes no local echo file")
	void rankedMenuSaveUploadsWithoutLocalFile() throws Exception {
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(201, "{}");
		EchoOnlineSettings.setOnlineEnabled(true);
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");
		EchoPlayerSession.applyAuthResponse("jwt", "Hero", false, null);
		EchoOnlineSync sync = new EchoOnlineSync(
				new EchoClient("https://echo.test", "secret", transport));
		EchoOnlineSync.setDefaultForTests(sync);

		Echo echo = EchoTestSupport.warriorEchoWithData(5);
		echo.echoId = "manual-ranked";

		WndGame.saveRankedEcho(echo);
		sync.awaitBackgroundTasksForTests();

		Assertions.assertThat(transport.requests).hasSize(1);
		Assertions.assertThat(transport.requests.get(0).url).endsWith("/v1/echoes");
		Assertions.assertThat(transport.requests.get(0).body).contains("manual-ranked");
		Assertions.assertThat(FileUtils.fileExists("echoes-ranked/depth-5.dat")).isFalse();
		Assertions.assertThat(EchoTestSupport.countEchoFiles()).isZero();
	}
}
