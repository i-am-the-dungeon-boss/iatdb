package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.watabou.utils.Bundle;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoPolicyTest {

	private static final String ROLE_POLICY = "{"
			+ "\"policy_schema_version\":\"0.0.1\","
			+ "\"capabilities\":{\"MELEE\":{\"pick\":\"FIRST_LEGAL\",\"items\":[\"*melee\"]}},"
			+ "\"reactions\":[],"
			+ "\"recipes\":[],"
			+ "\"positioning\":{\"DEFAULT\":{\"ideal_distance\":1}},"
			+ "\"matchups\":{},"
			+ "\"selection\":{\"order\":[\"reactions\",\"default\"],\"default_roles\":[\"MELEE\"]},"
			+ "\"tuning\":{\"aggression\":0.7}"
			+ "}";

	@Test
	@DisplayName("role-based policy with capabilities is supported")
	void roleBasedPolicyIsSupported() {
		EchoPolicy policy = EchoPolicy.fromJson(ROLE_POLICY);

		Assertions.assertThat(policy.isSupported()).isTrue();
		Assertions.assertThat(policy.root().has("capabilities")).isTrue();
	}

	@Test
	@DisplayName("bundle round-trip preserves full role-based policy JSON")
	void bundleRoundTripPreservesRolePolicy() {
		EchoPolicy original = EchoPolicy.fromJson(ROLE_POLICY);

		EchoPolicy restored = EchoPolicy.fromBundle(original.toBundle());

		Assertions.assertThat(restored.root().toString())
				.isEqualTo(original.root().toString());
		Assertions.assertThat(restored.root().getJSONObject("capabilities").has("MELEE")).isTrue();
	}

	@Test
	@DisplayName("schema version is ignored for support checks")
	void schemaVersionIsIgnoredForSupport() {
		EchoPolicy policy = EchoPolicy.fromJson("{"
				+ "\"policy_schema_version\":\"9.9.9\","
				+ "\"capabilities\":{\"MELEE\":{\"pick\":\"FIRST_LEGAL\",\"items\":[\"*melee\"]}}"
				+ "}");

		Assertions.assertThat(policy.isSupported()).isTrue();
	}

	@Test
	@DisplayName("policy without capabilities is not supported")
	void policyWithoutCapabilitiesIsNotSupported() {
		EchoPolicy policy = EchoPolicy.fromJson("{"
				+ "\"policy_schema_version\":\"0.0.1\""
				+ "}");

		Assertions.assertThat(policy.isSupported()).isFalse();
	}

	@Test
	@DisplayName("fallback policy is role-based and supported")
	void fallbackIsRoleBasedAndSupported() {
		EchoPolicy policy = EchoPolicy.fallback();

		Assertions.assertThat(policy.isSupported()).isTrue();
		Assertions.assertThat(policy.root().has("capabilities")).isTrue();
		Assertions.assertThat(policy.schemaVersion).isEqualTo("0.0.1");
	}

	@Test
	@DisplayName("fromBundle rejects missing policy_json")
	void fromBundleRejectsMissingPolicyJson() {
		Assertions.assertThatThrownBy(() -> EchoPolicy.fromBundle(new Bundle()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("echo_policy");
	}

	@Test
	@DisplayName("numeric policy_schema_version is stored as empty string")
	void numericSchemaVersionBecomesEmpty() {
		EchoPolicy policy = EchoPolicy.fromJson("{"
				+ "\"policy_schema_version\":1,"
				+ "\"capabilities\":{\"MELEE\":{\"pick\":\"FIRST_LEGAL\",\"items\":[\"*melee\"]}}"
				+ "}");

		Assertions.assertThat(policy.schemaVersion).isEmpty();
		Assertions.assertThat(policy.isSupported()).isTrue();
	}
}
