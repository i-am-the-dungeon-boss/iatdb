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
		Echo echo = EchoTestSupport.warriorEchoWithData(5);

		String json = EchoWireCodec.encodeEchoUpload(echo, "test-client");

		Assertions.assertThat(json).contains("\"echo_id\":\"" + echo.echoId + "\"");
		Assertions.assertThat(json).contains("\"depth\":5");
		Assertions.assertThat(json).contains("\"hero_class\":\"WARRIOR\"");
		Assertions.assertThat(json).contains("\"echo_data_base64\":");
		Assertions.assertThat(json).contains("\"policy_input\":");
		Assertions.assertThat(json).contains("\"items\":");
	}

	@Test
	@DisplayName("encodes upload policy_input kit without duplicating hero_class or lvl")
	void encodesPolicyInputFromEchoHero() throws Exception {
		Echo echo = EchoTestSupport.warriorEchoWithData(5);

		org.json.JSONObject root = new org.json.JSONObject(
				EchoWireCodec.encodeEchoUpload(echo, "test-client"));
		org.json.JSONObject policyInput = root.getJSONObject("policy_input");

		Assertions.assertThat(root.getString("hero_class")).isEqualTo("WARRIOR");
		Assertions.assertThat(root.getInt("lvl")).isEqualTo(6);
		Assertions.assertThat(policyInput.has("hero_class")).isFalse();
		Assertions.assertThat(policyInput.has("lvl")).isFalse();
		Assertions.assertThat(policyInput.getString("subclass")).isEqualTo("NONE");
		Assertions.assertThat(policyInput.getJSONArray("items").length()).isGreaterThan(0);
		Assertions.assertThat(policyInput.getJSONArray("items").toString())
				.contains("WornShortsword");
		Assertions.assertThat(policyInput.has("talents")).isTrue();
	}

	@Test
	@DisplayName("encodes solo policy request from full policy_input")
	void encodesEchoPolicyRequestFromPolicyInput() {
		Echo echo = EchoTestSupport.warriorEchoWithData(5);
		EchoPolicyInput input = EchoPolicyInput.fromEcho(echo);

		String json = EchoWireCodec.encodeEchoPolicyRequest(input);

		Assertions.assertThat(json).contains("\"hero_class\":\"WARRIOR\"");
		Assertions.assertThat(json).contains("\"lvl\":6");
		Assertions.assertThat(json).contains("\"items\":");
		Assertions.assertThat(json).contains("\"talents\":");
		Assertions.assertThat(json).doesNotContain("echo_data_base64");
	}

	@Test
	@DisplayName("decodes echo fetch response with echo policy")
	void decodesEchoFetchResponse() throws Exception {
		Echo echo = EchoTestSupport.warriorEchoWithData(5);
		echo.echoId = "5-1";
		String json = EchoTestSupport.fetchResponseJson(echo, EchoPolicy.fallback());

		EchoFetchResult result = EchoWireCodec.decodeEchoFetch(json);

		Assertions.assertThat(result.echo.echoId).isEqualTo("5-1");
		Assertions.assertThat(result.echo.hasCombatData()).isTrue();
		Assertions.assertThat(result.policy.schemaVersion).isEqualTo("0.0.1");
		Assertions.assertThat(result.policy.isSupported()).isTrue();
	}

	@Test
	@DisplayName("decodes user_name and kill_count from echo fetch response")
	void decodesUserNameAndKillCountFromFetch() throws Exception {
		Echo echo = EchoTestSupport.warriorEchoWithData(5);
		echo.echoId = "5-1";
		echo.userName = "Alex";
		echo.killCount = 7;
		String json = EchoTestSupport.fetchResponseJson(echo, EchoPolicy.fallback());

		EchoFetchResult result = EchoWireCodec.decodeEchoFetch(json);

		Assertions.assertThat(result.echo.userName).isEqualTo("Alex");
		Assertions.assertThat(result.echo.killCount).isEqualTo(7);
	}

	@Test
	@DisplayName("defaults kill_count to zero when fetch omits it")
	void defaultsKillCountWhenFetchOmitsIt() throws Exception {
		Echo echo = EchoTestSupport.warriorEchoWithData(5);
		echo.echoId = "5-1";
		org.json.JSONObject root = new org.json.JSONObject(
				EchoTestSupport.fetchResponseJson(echo, EchoPolicy.fallback()));
		root.remove("kill_count");

		EchoFetchResult result = EchoWireCodec.decodeEchoFetch(root.toString());

		Assertions.assertThat(result.echo.killCount).isZero();
	}

	@Test
	@DisplayName("keeps role-based echo_policy on fetch")
	void keepsRoleBasedPolicyOnFetch() throws Exception {
		Echo echo = EchoTestSupport.warriorEchoWithData(5);
		EchoPolicy policy = EchoTestSupport.roleBasedPolicy();
		String json = EchoTestSupport.fetchResponseJson(echo, policy);

		EchoFetchResult result = EchoWireCodec.decodeEchoFetch(json);

		Assertions.assertThat(result.policy.isSupported()).isTrue();
		Assertions.assertThat(result.policy.root().has("capabilities")).isTrue();
	}

	@Test
	@DisplayName("rejects echo fetch response with invalid echo_data_base64")
	void rejectsInvalidEchoData() {
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
				+ "\"policy_schema_version\":\"0.0.1\","
				+ "\"capabilities\":{\"MELEE\":{\"pick\":\"FIRST_LEGAL\",\"items\":[\"*melee\"]}}"
				+ "}"
				+ "}";

		Assertions.assertThatThrownBy(() -> EchoWireCodec.decodeEchoFetch(json))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("echo_data");
	}

	@Test
	@DisplayName("rejects echo fetch response without echo_data_base64")
	void rejectsMissingEchoData() {
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
				+ "\"echo_policy\":{"
				+ "\"policy_schema_version\":\"0.0.1\","
				+ "\"capabilities\":{\"MELEE\":{\"pick\":\"FIRST_LEGAL\",\"items\":[\"*melee\"]}}"
				+ "}"
				+ "}";

		Assertions.assertThatThrownBy(() -> EchoWireCodec.decodeEchoFetch(json))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("echo_data");
	}

	@Test
	@DisplayName("decodes role-based solo policy response without throwing")
	void decodesRoleBasedSoloPolicy() {
		String json = "{"
				+ "\"echo_policy\":{"
				+ "\"policy_schema_version\":\"0.0.1\","
				+ "\"capabilities\":{\"RANGED\":{\"pick\":\"MAX_DAMAGE\",\"items\":[\"MagesStaff\"]}},"
				+ "\"reactions\":[],"
				+ "\"recipes\":[],"
				+ "\"selection\":{\"order\":[\"default\"],\"default_roles\":[\"RANGED\"]}"
				+ "},"
				+ "\"base_policy_version\":\"0.0.1\""
				+ "}";

		EchoPolicy policy = EchoWireCodec.decodeEchoPolicyResponse(json);

		Assertions.assertThat(policy.root().getJSONObject("capabilities").has("RANGED")).isTrue();
		Assertions.assertThat(policy.isSupported()).isTrue();
	}

	@Test
	@DisplayName("rejects numeric game_version")
	void rejectsNumericGameVersion() throws Exception {
		Echo echo = EchoTestSupport.warriorEchoWithData(5);
		org.json.JSONObject root = new org.json.JSONObject(
				EchoTestSupport.fetchResponseJson(echo, EchoPolicy.fallback()));
		root.put("game_version", 1);

		Assertions.assertThatThrownBy(() -> EchoWireCodec.decodeEchoFetch(root.toString()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("game_version");
	}

	@Test
	@DisplayName("rejects echo fetch response without echo policy")
	void rejectsEchoFetchResponseWithoutPolicy() throws Exception {
		Echo echo = EchoTestSupport.warriorEchoWithData(5);
		org.json.JSONObject root = new org.json.JSONObject(
				EchoWireCodec.encodeEchoUpload(echo, "test-client"));

		Assertions.assertThatThrownBy(() -> EchoWireCodec.decodeEchoFetch(root.toString()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("echo_policy");
	}

	@Test
	@DisplayName("rejects echo fetch response with unsupported echo policy")
	void rejectsUnsupportedEchoPolicy() throws Exception {
		Echo echo = EchoTestSupport.warriorEchoWithData(5);
		EchoPolicy unsupported = EchoPolicy.fromJson("{"
				+ "\"policy_schema_version\":\"0.0.1\""
				+ "}");

		Assertions.assertThatThrownBy(
				() -> EchoWireCodec.decodeEchoFetch(EchoTestSupport.fetchResponseJson(echo, unsupported)))
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
	@DisplayName("rejects leaderboard encode when player class is missing")
	void rejectsLeaderboardEncodeWithoutPlayerClass() {
		Assertions.assertThatThrownBy(() -> EchoWireCodec.encodeLeaderboardResult(
				new com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoFightResult(
						"5-1", true, 5, 99L, "0.0.1", null, 40, 12, 20)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("player_class");
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

	@Test
	@DisplayName("skips leaderboard entries missing player_class")
	void skipsLeaderboardEntriesMissingPlayerClass() {
		String json = "[{"
				+ "\"rank\":1,"
				+ "\"boss_win\":true,"
				+ "\"depth\":5,"
				+ "\"damage_dealt\":40"
				+ "},{"
				+ "\"rank\":2,"
				+ "\"boss_win\":false,"
				+ "\"depth\":5,"
				+ "\"player_class\":\"WARRIOR\","
				+ "\"damage_dealt\":10"
				+ "}]";

		var entries = EchoWireCodec.decodeLeaderboardEntries(json);

		Assertions.assertThat(entries).hasSize(1);
		Assertions.assertThat(entries.get(0).playerClass).isEqualTo("WARRIOR");
	}
}
