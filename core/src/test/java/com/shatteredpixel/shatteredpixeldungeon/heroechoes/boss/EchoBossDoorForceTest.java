package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicyStatus;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicyStatusBuilder;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The door dance: the hero ducks behind a door, the echo follows, the policy
 * then backs it off again, the door shuts, and the pair repeat that forever.
 * The echo answers it itself, without waiting for a playbook reaction: it
 * knocks twice, then spends a DOOR_BREAK item on the door. A kit with nothing
 * for the job gets no answer at all — the echo never destroys terrain for
 * free — but it keeps counting, so the door falls the turn an item is ready.
 */
@ExtendWith(GdxTestExtension.class)
class EchoBossDoorForceTest {

	@Test
	@DisplayName("a door found shut once is not smashed yet")
	void firstShutDoorIsNotSmashed() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = bossBehindDoor(hero);
		int door = doorCell(hero);

		boss.noteDoorPursuit();

		Assertions.assertThat(boss.doorStallTurns()).isEqualTo(1);
		Assertions.assertThat(boss.forceStalledDoor(status(boss))).isFalse();
		Assertions.assertThat(Dungeon.level.map[door]).isEqualTo(Terrain.DOOR);
	}

	@Test
	@DisplayName("with nothing in the kit the door survives — no free demolition")
	void nothingInKitLeavesTheDoorStanding() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = bossBehindDoor(hero);
		int door = doorCell(hero);

		boss.noteDoorPursuit();
		boss.noteDoorPursuit();

		Assertions.assertThat(boss.forceStalledDoor(status(boss))).isFalse();
		Assertions.assertThat(Dungeon.level.map[door]).isEqualTo(Terrain.DOOR);
	}

	@Test
	@DisplayName("the pressure is kept while the kit is empty, so a later charge still ends it")
	void pressureSurvivesAnEmptyKit() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = bossBehindDoor(hero, fireblastPolicy());
		int door = doorCell(hero);

		boss.noteDoorPursuit();
		boss.noteDoorPursuit();
		Assertions.assertThat(boss.forceStalledDoor(status(boss, fireblastPolicy()))).isFalse();
		Assertions.assertThat(boss.doorStallCell()).isEqualTo(door);
		Assertions.assertThat(boss.doorStallTurns()).isGreaterThanOrEqualTo(2);

		// The wand comes back up; nothing else about the situation changed.
		WandOfFireblast wand = new WandOfFireblast();
		wand.identify();
		wand.curCharges = 3;
		wand.collect(boss.getEchoHero().belongings.backpack);

		Assertions.assertThat(boss.forceStalledDoor(status(boss, fireblastPolicy()))).isTrue();
		Assertions.assertThat(Dungeon.level.map[door]).isNotEqualTo(Terrain.DOOR);
		Assertions.assertThat(boss.doorStallCell()).isEqualTo(-1);
		Assertions.assertThat(boss.doorStallTurns()).isZero();
	}

	@Test
	@DisplayName("a full dance cycle — follow through, kite back out, shut again — smashes it")
	void danceCycleSmashesTheDoor() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = bossBehindDoor(hero, fireblastPolicy());
		int door = doorCell(hero);
		WandOfFireblast wand = new WandOfFireblast();
		wand.identify();
		wand.curCharges = 3;
		wand.collect(boss.getEchoHero().belongings.backpack);
		int away = boss.pos + 1;

		// Turn 1: the echo walks up to the shut door.
		boss.noteDoorPursuit();
		// Turn 2: it steps onto the door, which opens under it — no denial here.
		Dungeon.level.map[door] = Terrain.OPEN_DOOR;
		Dungeon.level.buildFlagMaps();
		boss.pos = door;
		boss.noteDoorPursuit();
		Assertions.assertThat(boss.doorStallTurns()).isEqualTo(1);
		// Turn 3: the policy kites it back off the doorway and the door shuts.
		boss.pos = away;
		Dungeon.level.map[door] = Terrain.DOOR;
		Dungeon.level.buildFlagMaps();
		boss.noteDoorPursuit();
		// Turn 4: it is back at the same shut door — second denial.
		boss.pos = adjacentTo(door);
		boss.noteDoorPursuit();

		Assertions.assertThat(boss.forceStalledDoor(status(boss, fireblastPolicy()))).isTrue();
		Assertions.assertThat(Dungeon.level.map[door]).isNotEqualTo(Terrain.DOOR);
	}

	@Test
	@DisplayName("a kit fire tool is spent on the door rather than a bare-handed smash")
	void kitItemIsSpentOnTheDoor() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = bossBehindDoor(hero, fireblastPolicy());
		int door = doorCell(hero);
		WandOfFireblast wand = new WandOfFireblast();
		wand.identify();
		wand.curCharges = 3;
		wand.collect(boss.getEchoHero().belongings.backpack);

		boss.noteDoorPursuit();
		boss.noteDoorPursuit();

		Assertions.assertThat(boss.forceStalledDoor(status(boss, fireblastPolicy()))).isTrue();
		Assertions.assertThat(wand.curCharges).isLessThan(3);
		// Fireblast knocks the door open on the spot and leaves Fire on it; the
		// blob burns the frame to embers on its own turn, so the doorway is not
		// something the hero can shut again.
		Assertions.assertThat(Dungeon.level.map[door]).isNotEqualTo(Terrain.DOOR);
		Assertions.assertThat(Dungeon.level.losBlocking[door]).isFalse();
		Assertions.assertThat(Blob.volumeAt(door, Fire.class)).isGreaterThan(0);
	}

	@Test
	@DisplayName("a bomb next to the echo is never the door tool")
	void bombIsNotSpentOnAnAdjacentDoor() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = bossBehindDoor(hero, bombPolicy());
		int door = doorCell(hero);
		Bomb bomb = new Bomb();
		bomb.collect(boss.getEchoHero().belongings.backpack);

		boss.noteDoorPursuit();
		boss.noteDoorPursuit();

		// Throwing it at a cell the echo is standing next to would put the blast
		// on itself, so the bomb is not the answer and nothing else is either.
		Assertions.assertThat(boss.forceStalledDoor(status(boss, bombPolicy()))).isFalse();
		Assertions.assertThat(Dungeon.level.map[door]).isEqualTo(Terrain.DOOR);
		Assertions.assertThat(boss.getEchoHero().belongings.backpack.contains(bomb)).isTrue();
	}

	@Test
	@DisplayName("the backend can raise the knock count")
	void tuningRaisesTheKnockCount() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = patientPolicy();
		EchoBoss boss = bossBehindDoor(hero, policy);
		int door = doorCell(hero);
		WandOfFireblast wand = new WandOfFireblast();
		wand.identify();
		wand.curCharges = 3;
		wand.collect(boss.getEchoHero().belongings.backpack);

		boss.noteDoorPursuit();
		boss.noteDoorPursuit();

		Assertions.assertThat(boss.forceStalledDoor(status(boss, policy))).isFalse();
		Assertions.assertThat(Dungeon.level.map[door]).isEqualTo(Terrain.DOOR);

		boss.noteDoorPursuit();

		Assertions.assertThat(boss.forceStalledDoor(status(boss, policy))).isTrue();
		Assertions.assertThat(Dungeon.level.map[door]).isNotEqualTo(Terrain.DOOR);
	}

	@Test
	@DisplayName("an unforceable door does not cost the turn — act() falls through to the policy")
	void unforceableDoorFallsThroughToTheNextLegalAction() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = bossBehindDoor(hero);
		int door = doorCell(hero);

		boss.noteDoorPursuit();
		boss.noteDoorPursuit();

		// forceStalledDoor declines rather than ending the turn, so act() runs on
		// into match / execute / the Java floor / mob hunting AI and one of them
		// spends it. What it picks is those phases' business; what matters here
		// is that the door check never swallows the turn.
		Assertions.assertThat(act(boss)).isTrue();
		Assertions.assertThat(boss.cooldown())
				.as("a later phase paid for the turn; the door check spent nothing")
				.isGreaterThan(0f);
		Assertions.assertThat(Dungeon.level.map[door]).isEqualTo(Terrain.DOOR);
		Assertions.assertThat(boss.doorStallCell()).isEqualTo(door);
	}

	@Test
	@DisplayName("a different door starts its own count")
	void differentDoorResetsCount() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = bossBehindDoor(hero);
		int door = doorCell(hero);
		int otherDoor = boss.pos + Dungeon.level.width();

		boss.noteDoorPursuit();
		Dungeon.level.map[door] = Terrain.EMPTY;
		Dungeon.level.map[otherDoor] = Terrain.DOOR;
		Dungeon.level.buildFlagMaps();
		hero.pos = otherDoor + Dungeon.level.width();
		boss.noteEnemySeenAt(hero.pos);
		boss.noteDoorPursuit();

		Assertions.assertThat(boss.doorStallCell()).isEqualTo(otherDoor);
		Assertions.assertThat(boss.doorStallTurns()).isEqualTo(1);
		Assertions.assertThat(boss.forceStalledDoor(status(boss))).isFalse();
	}

	@Test
	@DisplayName("a door the echo is not standing next to is never tracked")
	void distantDoorIsNotTracked() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = bossBehindDoor(hero);
		boss.pos = boss.pos + 2 * Dungeon.level.width();

		boss.noteDoorPursuit();

		Assertions.assertThat(boss.doorStallCell()).isEqualTo(-1);
		Assertions.assertThat(boss.forceStalledDoor(status(boss))).isFalse();
	}

	@Test
	@DisplayName("a locked door is left alone")
	void lockedDoorIsLeftAlone() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = bossBehindDoor(hero);
		int door = doorCell(hero);
		Dungeon.level.map[door] = Terrain.LOCKED_DOOR;
		Dungeon.level.buildFlagMaps();

		boss.noteDoorPursuit();
		boss.noteDoorPursuit();

		Assertions.assertThat(boss.doorStallCell()).isEqualTo(-1);
		Assertions.assertThat(boss.forceStalledDoor(status(boss))).isFalse();
		Assertions.assertThat(Dungeon.level.map[door]).isEqualTo(Terrain.LOCKED_DOOR);
	}

	/** {@code Mob.act()} is protected and this test lives in another package. */
	private static boolean act(EchoBoss boss) {
		try {
			java.lang.reflect.Method method = EchoBoss.class.getDeclaredMethod("act");
			method.setAccessible(true);
			return (Boolean) method.invoke(boss);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static EchoPolicyStatus status(EchoBoss boss) {
		return status(boss, meleePolicy());
	}

	private static EchoPolicyStatus status(EchoBoss boss, EchoPolicy policy) {
		return EchoPolicyStatusBuilder.build(boss, policy);
	}

	/** Hero in the room, one shut door between it and the echo. */
	private static EchoBoss bossBehindDoor(Hero hero) {
		return bossBehindDoor(hero, meleePolicy());
	}

	private static EchoBoss bossBehindDoor(Hero hero, EchoPolicy policy) {
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		int door = doorCell(hero);
		Dungeon.level.map[door] = Terrain.DOOR;
		Dungeon.level.buildFlagMaps();
		boss.noteEnemySeenAt(hero.pos);
		return boss;
	}

	private static int doorCell(Hero hero) {
		return hero.pos + 1;
	}

	/** Any cell next to the door that is not the door itself. */
	private static int adjacentTo(int door) {
		return door + Dungeon.level.width();
	}

	/** No DOOR_BREAK capability at all — nothing can force the door. */
	private static EchoPolicy meleePolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("MELEE", EchoTestSupport.capability("*melee"))
				.put("CLOSE_IN", EchoTestSupport.capability("*move_closer")));
	}

	/** DOOR_BREAK offering only a bomb — unusable from an adjacent cell. */
	private static EchoPolicy bombPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("DOOR_BREAK", EchoTestSupport.capability("Bomb"))
				.put("MELEE", EchoTestSupport.capability("*melee"))
				.put("CLOSE_IN", EchoTestSupport.capability("*move_closer")));
	}

	/** Backend asking for three knocks instead of the built-in two. */
	private static EchoPolicy patientPolicy() {
		return EchoTestSupport.policyWithCapabilities(
				new JSONObject()
						.put("DOOR_BREAK", EchoTestSupport.capability("WandOfFireblast"))
						.put("MELEE", EchoTestSupport.capability("*melee"))
						.put("CLOSE_IN", EchoTestSupport.capability("*move_closer")),
				new JSONObject().put("door_force_turns", 3));
	}

	private static EchoPolicy fireblastPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("DOOR_BREAK", EchoTestSupport.capability("WandOfFireblast"))
				.put("MELEE", EchoTestSupport.capability("*melee"))
				.put("CLOSE_IN", EchoTestSupport.capability("*move_closer")));
	}
}
