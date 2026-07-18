package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;

@ExtendWith(GdxTestExtension.class)
class EchoWireCodecTest {

	@Test
	@DisplayName("encodes echo upload fields required by the backend")
	void encodesEchoUploadPayload() throws Exception {
		Echo echo = EchoTestSupport.warriorEcho(5);

		String json = EchoWireCodec.encodeEchoUpload(echo, "test-client");

		Assertions.assertThat(json).contains("\"echo_id\":\"" + echo.echoId + "\"");
		Assertions.assertThat(json).contains("\"depth\":5");
		Assertions.assertThat(json).contains("\"hero_class\":\"WARRIOR\"");
		Assertions.assertThat(json).contains("\"echo_data_base64\":");
	}

	@Test
	@DisplayName("decodes echo fetch response with echo policy")
	void decodesEchoFetchResponse() throws Exception {
		String json = "{"
				+ "\"echo_id\":\"5-1\","
				+ "\"depth\":5,"
				+ "\"game_version\":\"0.0.1\","
				+ "\"hero_class\":\"MAGE\","
				+ "\"lvl\":6,"
				+ "\"hp\":20,"
				+ "\"ht\":30,"
				+ "\"timestamp\":100,"
				+ "\"game_seed\":42,"
				+ "\"echo_data_base64\":\"dGVzdA==\","
				+ "\"echo_policy\":{"
				+ "\"policy_schema_version\":1,"
				+ "\"rules\":[{\"when\":{},\"do\":{\"action\":\"MELEE_CHASE\"},\"priority\":0}]"
				+ "}"
				+ "}";

		EchoFetchResult result = EchoWireCodec.decodeEchoFetch(json);

		Assertions.assertThat(result.echo.echoId).isEqualTo("5-1");
		Assertions.assertThat(result.echo.heroClass).isEqualTo("MAGE");
		Assertions.assertThat(result.policy.schemaVersion).isEqualTo(1);
		Assertions.assertThat(result.policy.rules).hasSize(1);
	}

	@Test
	@DisplayName("rejects echo fetch response without echo policy")
	void rejectsEchoFetchResponseWithoutPolicy() {
		String json = "{"
				+ "\"echo_id\":\"5-1\","
				+ "\"depth\":5,"
				+ "\"game_version\":\"0.0.1\","
				+ "\"hero_class\":\"MAGE\","
				+ "\"lvl\":6,"
				+ "\"hp\":20,"
				+ "\"ht\":30,"
				+ "\"timestamp\":100,"
				+ "\"game_seed\":42,"
				+ "\"echo_data_base64\":\"dGVzdA==\""
				+ "}";

		Assertions.assertThatThrownBy(() -> EchoWireCodec.decodeEchoFetch(json))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("echo_policy");
	}

	@Test
	@DisplayName("encodes leaderboard fight results for upload")
	void encodesLeaderboardResult() {
		String json = EchoWireCodec.encodeLeaderboardResult(
				new com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoFightResult(
						"5-1", true, 5, 99L, "0.0.1", "MAGE", 40, 12, 20));

		Assertions.assertThat(json).contains("\"boss_win\":true");
		Assertions.assertThat(json).contains("\"player_class\":\"MAGE\"");
		Assertions.assertThat(json).contains("\"damage_dealt\":40");
	}

	@Test
	@DisplayName("decodes leaderboard entries from JSON array")
	void decodesLeaderboardEntries() {
		String json = "[{"
				+ "\"rank\":1,"
				+ "\"echo_id\":\"5-1\","
				+ "\"boss_win\":true,"
				+ "\"depth\":5,"
				+ "\"player_class\":\"MAGE\","
				+ "\"damage_dealt\":40,"
				+ "\"damage_taken\":12,"
				+ "\"turns\":20,"
				+ "\"win_rate_proxy\":1"
				+ "}]";

		var entries = EchoWireCodec.decodeLeaderboardEntries(json);

		Assertions.assertThat(entries).hasSize(1);
		Assertions.assertThat(entries.get(0).rank).isEqualTo(1);
		Assertions.assertThat(entries.get(0).echoId).isEqualTo("5-1");
		Assertions.assertThat(entries.get(0).playerClass).isEqualTo("MAGE");
		Assertions.assertThat(entries.get(0).winRateProxy).isEqualTo(1f);
	}
}
