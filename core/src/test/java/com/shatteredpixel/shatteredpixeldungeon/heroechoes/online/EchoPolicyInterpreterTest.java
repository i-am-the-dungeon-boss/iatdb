package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EchoPolicyInterpreterTest {

	@Test
	@DisplayName("selects highest-priority matching rule")
	void selectsHighestPriorityRule() {
		EchoPolicy policy = EchoPolicy.fromJson("{"
				+ "\"policy_schema_version\":1,"
				+ "\"rules\":["
				+ "{\"when\":{\"self_hp_below\":0.5},\"do\":{\"action\":\"WAIT\"},\"priority\":10},"
				+ "{\"when\":{\"self_hp_below\":0.4},\"do\":{\"action\":\"USE_ITEM\",\"item\":\"PotionOfHealing\"},\"priority\":100},"
				+ "{\"when\":{},\"do\":{\"action\":\"MELEE_CHASE\"},\"priority\":0}"
				+ "]"
				+ "}");

		EchoPolicyContext context = new EchoPolicyContext()
				.selfHpRatio(0.3f)
				.hasItem("PotionOfHealing", 1);

		EchoPolicyAction action = EchoPolicyInterpreter.interpret(policy, context);

		Assertions.assertThat(action.type).isEqualTo(EchoPolicyAction.Type.USE_ITEM);
		Assertions.assertThat(action.item).isEqualTo("PotionOfHealing");
	}

	@Test
	@DisplayName("falls back to bundled policy when server policy is unsupported")
	void fallsBackWhenUnsupported() {
		EchoPolicy policy = EchoPolicy.fromJson("{ \"policy_schema_version\": 99, \"rules\": [] }");

		EchoPolicyAction action = EchoPolicyInterpreter.interpret(
				policy,
				new EchoPolicyContext().selfHpRatio(1f)
		);

		Assertions.assertThat(action.type).isEqualTo(EchoPolicyAction.Type.MELEE_CHASE);
	}

	@Test
	@DisplayName("maps policy actions to echo boss intended actions")
	void mapsToEchoBossIntendedAction() {
		Assertions.assertThat(
				EchoPolicyInterpreter.toIntendedAction(new EchoPolicyAction(EchoPolicyAction.Type.USE_ITEM, null, 0))
		).isEqualTo(EchoBoss.IntendedAction.HEAL);

		Assertions.assertThat(
				EchoPolicyInterpreter.toIntendedAction(new EchoPolicyAction(EchoPolicyAction.Type.KEEP_DISTANCE, null, 3))
		).isEqualTo(EchoBoss.IntendedAction.MOVE);

		Assertions.assertThat(
				EchoPolicyInterpreter.toIntendedAction(new EchoPolicyAction(EchoPolicyAction.Type.MELEE_CHASE, null, 0))
		).isEqualTo(EchoBoss.IntendedAction.ATTACK);
	}
}
