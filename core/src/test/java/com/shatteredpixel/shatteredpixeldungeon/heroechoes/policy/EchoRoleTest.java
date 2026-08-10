package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Invariants for the role registry. These exist because the facts in
 * {@link EchoRole} used to live as duplicated literals and hand-kept arrays —
 * these assertions are what stops them drifting apart again.
 */
public class EchoRoleTest {

	@Test
	@DisplayName("every role resolves back from its own wire id")
	public void everyRoleResolvesFromItsWireId() {
		EchoRole[] all = EchoRole.values();
		for (int i = 0; i < all.length; i++) {
			assertThat(EchoRole.byId(all[i].id()))
					.as("round trip for %s", all[i].id())
					.isSameAs(all[i]);
		}
	}

	@Test
	@DisplayName("unknown role ids resolve to null instead of throwing")
	public void unknownRolesResolveToNullRatherThanThrowing() {
		assertThat(EchoRole.byId("NOT_A_ROLE")).isNull();
		assertThat(EchoRole.byId(null)).isNull();
		assertThat(EchoRole.isKnown("NOT_A_ROLE")).isFalse();
	}

	@Test
	@DisplayName("prep ranks are dense and unique so PREP_ORDER has no holes")
	public void prepRanksAreDenseAndUniqueSoPrepOrderHasNoHoles() {
		Set<Integer> ranks = new HashSet<>();
		int highest = 0;
		EchoRole[] all = EchoRole.values();
		for (int i = 0; i < all.length; i++) {
			if (!all[i].isPrep()) {
				continue;
			}
			assertThat(ranks.add(all[i].prepRank()))
					.as("duplicate prep rank %d on %s", all[i].prepRank(), all[i].id())
					.isTrue();
			highest = Math.max(highest, all[i].prepRank());
		}
		assertThat(ranks).hasSize(highest);
		assertThat(EchoUntouchable.PREP_ORDER).hasSize(highest).doesNotContainNull();
	}

	@Test
	@DisplayName("prep order leads with knockback, then crowd control")
	public void prepOrderLeadsWithKnockbackThenCrowdControl() {
		assertThat(EchoUntouchable.PREP_ORDER[0]).isEqualTo(EchoRole.KNOCKBACK.id());
		assertThat(EchoUntouchable.PREP_ORDER[1]).isEqualTo(EchoRole.SETUP_CC.id());
	}

	@Test
	@DisplayName("damage roles are the ones the retreat stances gate")
	public void damageRolesAreExactlyTheOnesTheRetreatStancesGate() {
		assertThat(EchoRole.MELEE.isDamage()).isTrue();
		assertThat(EchoRole.PATH_THROUGH.isDamage()).isTrue();
		assertThat(EchoRole.PAYOFF_AOE.isDamage()).isTrue();
		assertThat(EchoRole.KEEP_DISTANCE.isDamage()).isFalse();
		assertThat(EchoRole.HEAL.isDamage()).isFalse();
	}

	@Test
	@DisplayName("PAYOFF_AOE counts as both damage and blob")
	public void payoffAoeCountsAsBothDamageAndBlob() {
		assertThat(EchoRole.PAYOFF_AOE.isDamage()).isTrue();
		assertThat(EchoRole.PAYOFF_AOE.isBlob()).isTrue();
		assertThat(EchoRole.SETUP_CC.isBlob()).isTrue();
		assertThat(EchoRole.SETUP_CC.isDamage()).isFalse();
	}

	@Test
	@DisplayName("a potion role is either drink or throw, never both")
	public void potionRolesAreEitherDrinkOrThrowButNeverBoth() {
		EchoRole[] all = EchoRole.values();
		for (int i = 0; i < all.length; i++) {
			assertThat(all[i].drinksPotion() && all[i].throwsPotion())
					.as("%s claims both potion modes", all[i].id())
					.isFalse();
		}
		assertThat(EchoRole.SHIELD_SELF.drinksPotion()).isTrue();
		assertThat(EchoRole.GAS.throwsPotion()).isTrue();
	}

	@Test
	@DisplayName("virtual tags are unique and start with the tag marker")
	public void virtualTagsAreUniqueAndStartWithTheTagMarker() {
		Set<String> tags = new HashSet<>();
		EchoRole[] all = EchoRole.values();
		for (int i = 0; i < all.length; i++) {
			String tag = all[i].virtualTag();
			if (tag == null) {
				continue;
			}
			assertThat(tag).as("virtual tag for %s", all[i].id()).startsWith("*");
			assertThat(tags.add(tag)).as("duplicate virtual tag %s", tag).isTrue();
		}
		assertThat(EchoRole.MELEE.virtualTag()).isEqualTo("*melee");
		assertThat(EchoRole.HOLD.virtualTag()).isEqualTo("*wait");
	}

	@Test
	@DisplayName("hazard constants still spell the same ids as the registry")
	public void hazardConstantsStillSpellTheSameIdsAsTheRegistry() {
		assertThat(EchoPolicyHazards.SETUP_CC).isEqualTo(EchoRole.SETUP_CC.id());
		assertThat(EchoPolicyHazards.KEEP_DISTANCE).isEqualTo(EchoRole.KEEP_DISTANCE.id());
		assertThat(EchoPolicyHazards.SHIELD_SELF).isEqualTo(EchoRole.SHIELD_SELF.id());
	}
}
