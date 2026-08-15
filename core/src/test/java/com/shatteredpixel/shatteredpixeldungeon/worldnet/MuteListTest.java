package com.shatteredpixel.shatteredpixeldungeon.worldnet;

import static org.assertj.core.api.Assertions.assertThat;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
@DisplayName("Client-side mute list")
class MuteListTest {

	@BeforeEach
	void setUp() {
		MuteList.clear();
	}

	@Test
	@DisplayName("nobody is muted to begin with")
	void startsEmpty() {
		assertThat(MuteList.muted()).isEmpty();
		assertThat(MuteList.isMuted("Alice")).isFalse();
	}

	@Test
	@DisplayName("mutes a player by username")
	void mutesByUsername() {
		MuteList.mute("Alice");

		assertThat(MuteList.isMuted("Alice")).isTrue();
	}

	@Test
	@DisplayName("matches a muted name regardless of case")
	void matchesCaseInsensitively() {
		MuteList.mute("Alice");

		assertThat(MuteList.isMuted("alice")).isTrue();
		assertThat(MuteList.isMuted("ALICE")).isTrue();
	}

	@Test
	@DisplayName("ignores surrounding whitespace when muting and matching")
	void ignoresPadding() {
		MuteList.mute("  Alice  ");

		assertThat(MuteList.isMuted("Alice")).isTrue();
	}

	@Test
	@DisplayName("leaves other players unmuted")
	void doesNotOverreach() {
		MuteList.mute("Alice");

		assertThat(MuteList.isMuted("Bob")).isFalse();
	}

	@Test
	@DisplayName("unmutes a player")
	void unmutes() {
		MuteList.mute("Alice");

		MuteList.unmute("Alice");

		assertThat(MuteList.isMuted("Alice")).isFalse();
		assertThat(MuteList.muted()).isEmpty();
	}

	@Test
	@DisplayName("muting the same name twice does not duplicate the entry")
	void doesNotDuplicate() {
		MuteList.mute("Alice");
		MuteList.mute("alice");

		assertThat(MuteList.muted()).hasSize(1);
	}

	@Test
	@DisplayName("survives being reloaded from settings")
	void persists() {
		MuteList.mute("Alice");
		MuteList.mute("Bob");

		MuteList.reload();

		assertThat(MuteList.muted()).containsExactlyInAnyOrder("alice", "bob");
	}

	@Test
	@DisplayName("a name containing the separator cannot corrupt the stored list")
	void rejectsSeparatorInName() {
		MuteList.mute("Alice,Bob");

		MuteList.reload();

		assertThat(MuteList.isMuted("Alice")).isFalse();
		assertThat(MuteList.isMuted("Bob")).isFalse();
	}

	@Test
	@DisplayName("ignores a blank name rather than muting everyone")
	void ignoresBlankName() {
		MuteList.mute("   ");

		assertThat(MuteList.muted()).isEmpty();
		assertThat(MuteList.isMuted("")).isFalse();
	}

	@Test
	@DisplayName("stops growing once the cap is reached")
	void capsTheList() {
		for (int i = 0; i < MuteList.CAP + 10; i++) {
			MuteList.mute("player" + i);
		}

		assertThat(MuteList.muted()).hasSize(MuteList.CAP);
	}
}
