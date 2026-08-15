package com.shatteredpixel.shatteredpixeldungeon.worldnet.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldChatMessage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The filter that keeps the chat log from repeating itself.
 *
 * <p>The socket is rotated every few minutes by design, and every reconnect
 * replays the server's recent backlog. Without this the player would watch the
 * same conversation scroll past again on a timer.
 */
@DisplayName("World chat backlog")
class ChatBacklogTest {

	private static WorldChatMessage message(String id) {
		return new WorldChatMessage(id, "p1", "One", "hello", 0L);
	}

	private static List<String> idsOf(List<WorldChatMessage> messages) {
		List<String> ids = new ArrayList<>();
		for (WorldChatMessage message : messages) {
			ids.add(message.id);
		}
		return ids;
	}

	@Test
	@DisplayName("passes messages it has never seen through in order")
	void freshMessagesArriveUntouched() {
		ChatBacklog backlog = new ChatBacklog();

		assertThat(idsOf(backlog.fresh(Arrays.asList(message("a"), message("b")))))
				.containsExactly("a", "b");
	}

	@Test
	@DisplayName("drops the replayed part of a backlog and keeps the rest")
	void aReplayedBacklogOnlyYieldsWhatIsNew() {
		ChatBacklog backlog = new ChatBacklog();
		backlog.fresh(Arrays.asList(message("a"), message("b")));

		assertThat(idsOf(backlog.fresh(Arrays.asList(message("a"), message("b"), message("c")))))
				.containsExactly("c");
	}

	@Test
	@DisplayName("shows nothing when the whole replay has already been seen")
	void anEntirelyRepeatedBacklogIsEmpty() {
		ChatBacklog backlog = new ChatBacklog();
		backlog.fresh(Arrays.asList(message("a")));

		assertThat(backlog.fresh(Arrays.asList(message("a")))).isEmpty();
	}

	@Test
	@DisplayName("still recognises the whole of a replay buffer after a long session")
	void theCapNeverForgetsAnythingTheServerCouldStillReplay() {
		ChatBacklog backlog = new ChatBacklog();
		// Far more than the cap, so the oldest ids have certainly been dropped.
		for (int i = 0; i < 1000; i++) {
			backlog.fresh(Arrays.asList(message("m" + i)));
		}

		// The server replays only its recent buffer, which is well inside what is
		// still remembered.
		List<WorldChatMessage> replay = new ArrayList<>();
		for (int i = 950; i < 1000; i++) {
			replay.add(message("m" + i));
		}
		assertThat(backlog.fresh(replay)).isEmpty();
	}

	@Test
	@DisplayName("starts over on a new session, since the log it fed was torn down too")
	void clearingShowsEverythingAgain() {
		ChatBacklog backlog = new ChatBacklog();
		backlog.fresh(Arrays.asList(message("a")));
		backlog.clear();

		assertThat(idsOf(backlog.fresh(Arrays.asList(message("a"))))).containsExactly("a");
	}
}
