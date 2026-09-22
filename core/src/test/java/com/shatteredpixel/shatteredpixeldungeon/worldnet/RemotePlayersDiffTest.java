package com.shatteredpixel.shatteredpixeldungeon.worldnet;

import static org.assertj.core.api.Assertions.assertThat;

import com.shatteredpixel.shatteredpixeldungeon.worldnet.ui.RemotePlayers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@DisplayName("Remote player reconciliation")
class RemotePlayersDiffTest {

	private static WorldPresence at(String playerId, int cell) {
		return new WorldPresence(playerId, "Name" + playerId, "WARRIOR", cell, 1);
	}

	private static Map<String, Integer> onScreen(String playerId, int cell) {
		Map<String, Integer> cells = new HashMap<>();
		cells.put(playerId, cell);
		return cells;
	}

	@Test
	@DisplayName("treats a player in the snapshot but not on screen as an arrival")
	void detectsArrival() {
		RemotePlayers.Diff diff = RemotePlayers.diff(new HashMap<>(), Collections.singletonList(at("p1", 10)));

		assertThat(diff.added).containsExactly("p1");
		assertThat(diff.moved).isEmpty();
		assertThat(diff.removed).isEmpty();
	}

	@Test
	@DisplayName("treats a player on screen but absent from the snapshot as a departure")
	void detectsDeparture() {
		RemotePlayers.Diff diff = RemotePlayers.diff(onScreen("p1", 10), new ArrayList<WorldPresence>());

		assertThat(diff.removed).containsExactly("p1");
		assertThat(diff.added).isEmpty();
	}

	@Test
	@DisplayName("treats a changed cell as a move, not a respawn")
	void detectsMove() {
		RemotePlayers.Diff diff = RemotePlayers.diff(onScreen("p1", 10), Collections.singletonList(at("p1", 11)));

		assertThat(diff.moved).containsExactly("p1");
		assertThat(diff.added).isEmpty();
		assertThat(diff.removed).isEmpty();
	}

	@Test
	@DisplayName("leaves a stationary player entirely alone")
	void ignoresStationaryPlayer() {
		RemotePlayers.Diff diff = RemotePlayers.diff(onScreen("p1", 10), Collections.singletonList(at("p1", 10)));

		assertThat(diff.added).isEmpty();
		assertThat(diff.moved).isEmpty();
		assertThat(diff.removed).isEmpty();
	}

	@Test
	@DisplayName("handles an arrival, a move and a departure in one snapshot")
	void handlesMixedSnapshot() {
		Map<String, Integer> current = new HashMap<>();
		current.put("stays", 5);
		current.put("moves", 6);
		current.put("leaves", 7);
		List<WorldPresence> snapshot = new ArrayList<>();
		snapshot.add(at("stays", 5));
		snapshot.add(at("moves", 60));
		snapshot.add(at("arrives", 8));

		RemotePlayers.Diff diff = RemotePlayers.diff(current, snapshot);

		assertThat(diff.added).containsExactly("arrives");
		assertThat(diff.moved).containsExactly("moves");
		assertThat(diff.removed).containsExactly("leaves");
	}

	@Test
	@DisplayName("empties the village when the roster comes back empty")
	void clearsOnEmptyRoster() {
		Map<String, Integer> current = new HashMap<>();
		current.put("p1", 1);
		current.put("p2", 2);

		RemotePlayers.Diff diff = RemotePlayers.diff(current, new ArrayList<WorldPresence>());

		assertThat(diff.removed).containsExactlyInAnyOrder("p1", "p2");
	}

	@Test
	@DisplayName("compares cells by value, so a boxed cell above the Integer cache still matches")
	void comparesCellsByValue() {
		// Cells beyond 127 fall outside Integer's cache; reference comparison
		// would report a spurious move on every single sync.
		RemotePlayers.Diff diff = RemotePlayers.diff(onScreen("p1", 5000), Collections.singletonList(at("p1", 5000)));

		assertThat(diff.moved).isEmpty();
	}

	private static WorldPresence typing(String playerId, boolean typing) {
		return new WorldPresence(playerId, "Name" + playerId, "WARRIOR", 10, 1, typing);
	}

	private static Set<String> showing(String playerId) {
		return Collections.singleton(playerId);
	}

	@Test
	@DisplayName("raises an indicator for a player who has started composing")
	void detectsTypingStarted() {
		RemotePlayers.TypingChanges changes = RemotePlayers.typingChanges(
				Collections.<String>emptySet(), Collections.singletonList(typing("p1", true)));

		assertThat(changes.started).containsExactly("p1");
		assertThat(changes.stopped).isEmpty();
	}

	@Test
	@DisplayName("clears the indicator once the player stops composing")
	void detectsTypingStopped() {
		RemotePlayers.TypingChanges changes = RemotePlayers.typingChanges(
				showing("p1"), Collections.singletonList(typing("p1", false)));

		assertThat(changes.stopped).containsExactly("p1");
		assertThat(changes.started).isEmpty();
	}

	@Test
	@DisplayName("leaves a still-composing player's indicator alone rather than rebuilding it")
	void ignoresUnchangedTyping() {
		RemotePlayers.TypingChanges changes = RemotePlayers.typingChanges(
				showing("p1"), Collections.singletonList(typing("p1", true)));

		assertThat(changes.started).isEmpty();
		assertThat(changes.stopped).isEmpty();
	}

	@Test
	@DisplayName("leaves a departed player out of the changes, since despawning takes the tag")
	void ignoresDepartedTyping() {
		RemotePlayers.TypingChanges changes = RemotePlayers.typingChanges(showing("p1"),
				new ArrayList<WorldPresence>());

		assertThat(changes.started).isEmpty();
		assertThat(changes.stopped).isEmpty();
	}
}
