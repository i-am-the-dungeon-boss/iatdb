package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.effects.Enchanting;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.sprites.EchoBossSprite;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * ANDROID-21: the paladin echo spells guarded on the body then drew on the kit,
 * so {@code Enchanting.show} dereferenced the kit's null sprite. The item was
 * right, the char was wrong — the glow belongs on the char that is on stage.
 */
@ExtendWith(GdxTestExtension.class)
class EchoClericEnchantTargetTest {

	private Stage stage;

	private EchoActionContext paladinEchoOnStage() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(boss.fieldOfView, true);

		EchoBossSprite sprite = new EchoBossSprite();
		sprite.ch = boss;
		boss.sprite = sprite;
		stage = new Stage();
		stage.add(sprite);

		EchoActionContext ctx = EchoActionContext.of(boss);
		Assertions.assertThat(ctx.canWorldFx())
				.as("the body must be world-visible or the FX branch never runs")
				.isTrue();
		return ctx;
	}

	private Char enchantTargetOnStage() throws Exception {
		Field target = Enchanting.class.getDeclaredField("target");
		target.setAccessible(true);
		for (Gizmo member : stage.snapshot()) {
			if (member instanceof Enchanting) {
				return (Char) target.get(member);
			}
		}
		return null;
	}

	/** {@code Group.members} is protected; a test-local subclass reads it. */
	private static final class Stage extends Group {
		Gizmo[] snapshot() {
			return members.toArray(new Gizmo[0]);
		}
	}

	@Test
	@DisplayName("Holy Weapon puts the enchant glow on the echo's body, not its kit")
	void holyWeaponEnchantsTheBody() throws Exception {
		EchoActionContext ctx = paladinEchoOnStage();
		ctx.stats().sprite = null;
		Assertions.assertThat(ctx.gear().weapon()).isNotNull();

		Assertions.assertThatCode(
				() -> EchoClericHandlers.cast(ctx, new HolyTome(), HolyWeapon.INSTANCE, null))
				.doesNotThrowAnyException();

		Assertions.assertThat(enchantTargetOnStage()).isSameAs(ctx.body);
	}

	@Test
	@DisplayName("Holy Ward puts the enchant glow on the echo's body, not its kit")
	void holyWardEnchantsTheBody() throws Exception {
		EchoActionContext ctx = paladinEchoOnStage();
		ctx.stats().sprite = null;
		Assertions.assertThat(ctx.gear().armor()).isNotNull();

		Assertions.assertThatCode(
				() -> EchoClericHandlers.cast(ctx, new HolyTome(), HolyWard.INSTANCE, null))
				.doesNotThrowAnyException();

		Assertions.assertThat(enchantTargetOnStage()).isSameAs(ctx.body);
	}
}
