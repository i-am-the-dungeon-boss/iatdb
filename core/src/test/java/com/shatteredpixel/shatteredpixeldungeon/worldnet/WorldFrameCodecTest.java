package com.shatteredpixel.shatteredpixeldungeon.worldnet;

import static org.assertj.core.api.Assertions.assertThat;

import com.shatteredpixel.shatteredpixeldungeon.village.VillageFigure;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.wire.WorldFrame;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.wire.WorldFrameCodec;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("World socket frame codec")
class WorldFrameCodecTest {

	@Test
	@DisplayName("encodes the local avatar as a presence frame")
	void encodesPresence() throws Exception {
		JSONObject frame =
				new JSONObject(WorldFrameCodec.encodePresence(WorldPresence.local("WARRIOR", 512, -1)));

		assertThat(frame.getString("t")).isEqualTo("presence");
		assertThat(frame.getString("hero_class")).isEqualTo("WARRIOR");
		assertThat(frame.getInt("cell")).isEqualTo(512);
		assertThat(frame.getInt("facing")).isEqualTo(-1);
	}

	@Test
	@DisplayName("encodes chat, focus, leave, ping and report frames")
	void encodesTheRemainingClientFrames() throws Exception {
		assertThat(new JSONObject(WorldFrameCodec.encodeChat("hi")).getString("text")).isEqualTo("hi");
		assertThat(new JSONObject(WorldFrameCodec.encodeFocus(true)).getBoolean("on")).isTrue();
		assertThat(new JSONObject(WorldFrameCodec.encodeLeave()).getString("t")).isEqualTo("leave");
		assertThat(new JSONObject(WorldFrameCodec.encodePing()).getString("t")).isEqualTo("ping");

		JSONObject report = new JSONObject(WorldFrameCodec.encodeReport("m1", "spam"));
		assertThat(report.getString("message_id")).isEqualTo("m1");
		assertThat(report.getString("reason")).isEqualTo("spam");
	}

	@Test
	@DisplayName("substitutes a safe reason rather than sending null")
	void defaultsAMissingReportReason() throws Exception {
		JSONObject report = new JSONObject(WorldFrameCodec.encodeReport("m1", null));

		assertThat(report.getString("reason")).isEqualTo("other");
	}

	@Test
	@DisplayName("decodes a chat frame into messages")
	void decodesChat() {
		WorldFrame frame = WorldFrameCodec.decode(
				"{\"t\":\"chat\",\"messages\":[{\"id\":\"m1\",\"name\":\"Ann\",\"text\":\"hi\",\"at\":7}]}");

		assertThat(frame.kind).isEqualTo(WorldFrame.Kind.CHAT);
		assertThat(frame.messages).hasSize(1);
		assertThat(frame.messages.get(0).text).isEqualTo("hi");
		assertThat(frame.messages.get(0).name).isEqualTo("Ann");
	}

	@Test
	@DisplayName("decodes a roster frame into occupants")
	void decodesRoster() {
		WorldFrame frame = WorldFrameCodec.decode(
				"{\"t\":\"roster\",\"occupants\":[{\"player_id\":\"p1\",\"name\":\"Ann\",\"hero_class\":\"MAGE\",\"cell\":9,\"facing\":-1}]}");

		assertThat(frame.kind).isEqualTo(WorldFrame.Kind.ROSTER);
		assertThat(frame.occupants).hasSize(1);
		assertThat(frame.occupants.get(0).cell).isEqualTo(9);
		assertThat(frame.occupants.get(0).facing).isEqualTo(-1);
	}

	@Test
	@DisplayName("decodes an occupant who is composing a chat message")
	void decodesTypingOccupant() {
		WorldFrame frame = WorldFrameCodec.decode(
				"{\"t\":\"roster\",\"occupants\":[{\"player_id\":\"p1\",\"name\":\"Ann\","
						+ "\"hero_class\":\"MAGE\",\"cell\":9,\"facing\":1,\"typing\":true}]}");

		assertThat(frame.occupants.get(0).typing).isTrue();
	}

	@Test
	@DisplayName("treats an occupant without a typing flag as not typing")
	void defaultsTypingToFalse() {
		// A server older than this build simply omits the field; the village must
		// read that as silence rather than refusing the occupant.
		WorldFrame frame = WorldFrameCodec.decode(
				"{\"t\":\"roster\",\"occupants\":[{\"player_id\":\"p1\",\"name\":\"Ann\","
						+ "\"hero_class\":\"MAGE\",\"cell\":9,\"facing\":1}]}");

		assertThat(frame.occupants.get(0).typing).isFalse();
	}

	@Test
	@DisplayName("decodes an empty roster, which is how the last player leaving is signalled")
	void decodesAnEmptyRoster() {
		WorldFrame frame = WorldFrameCodec.decode("{\"t\":\"roster\",\"occupants\":[]}");

		assertThat(frame.kind).isEqualTo(WorldFrame.Kind.ROSTER);
		assertThat(frame.occupants).isEmpty();
	}

	@Test
	@DisplayName("ignores a mute frame from a server older than this build")
	void ignoresLegacyMuteFrame() {
		// Mutes now arrive on the authentication response and are held locally, so
		// the socket has no mute frame. A server still sending one must not upset
		// a client that no longer understands it.
		assertThat(WorldFrameCodec.decode("{\"t\":\"muted\",\"muted_until\":99}").kind)
				.isEqualTo(WorldFrame.Kind.UNKNOWN);
	}

	@Test
	@DisplayName("decodes an error frame with its code")
	void decodesError() {
		WorldFrame frame =
				WorldFrameCodec.decode("{\"t\":\"error\",\"code\":\"rate_limited\",\"detail\":\"slow down\"}");

		assertThat(frame.kind).isEqualTo(WorldFrame.Kind.ERROR);
		assertThat(frame.errorCode).isEqualTo("rate_limited");
		assertThat(frame.detail).isEqualTo("slow down");
	}

	@Test
	@DisplayName("treats a frame type this build has never heard of as unknown, not an error")
	void decodesAnUnrecognisedFrameAsUnknown() {
		assertThat(WorldFrameCodec.decode("{\"t\":\"weather\",\"rain\":true}").kind)
				.isEqualTo(WorldFrame.Kind.UNKNOWN);
	}

	@Test
	@DisplayName("survives malformed JSON rather than tearing the connection down")
	void decodesMalformedJsonAsUnknown() {
		assertThat(WorldFrameCodec.decode("{not json").kind).isEqualTo(WorldFrame.Kind.UNKNOWN);
		assertThat(WorldFrameCodec.decode("").kind).isEqualTo(WorldFrame.Kind.UNKNOWN);
	}

	@Test
	@DisplayName("skips a malformed occupant rather than emptying the village")
	void skipsAMalformedOccupant() {
		// covered here since the polling codec, which used to own this case, is gone
		WorldFrame frame = WorldFrameCodec.decode(
				"{\"t\":\"roster\",\"occupants\":[{\"nope\":1},"
						+ "{\"player_id\":\"p2\",\"name\":\"Bee\",\"hero_class\":\"MAGE\","
						+ "\"cell\":7,\"facing\":1}]}");

		assertThat(frame.kind).isEqualTo(WorldFrame.Kind.ROSTER);
		assertThat(frame.occupants).hasSize(1);
		assertThat(frame.occupants.get(0).cell).isEqualTo(7);
	}

	@Test
	@DisplayName("skips a single malformed chat entry instead of dropping the whole frame")
	void skipsMalformedChatEntries() {
		WorldFrame frame = WorldFrameCodec.decode(
				"{\"t\":\"chat\",\"messages\":[{\"text\":\"no id\"},{\"id\":\"m2\",\"text\":\"kept\"}]}");

		assertThat(frame.messages).hasSize(1);
		assertThat(frame.messages.get(0).text).isEqualTo("kept");
	}
	private static final String FULL_FIGURE =
			"{\"post\":\"depth\",\"depth\":5,\"echo_id\":\"5-177\",\"user_name\":\"Somebody\","
					+ "\"hero_class\":\"WARRIOR\",\"armor_tier\":3,\"lvl\":14,\"hp\":30,\"ht\":40,"
					+ "\"kill_count\":7,\"timestamp\":1771000000000,"
					+ "\"badges\":[{\"kind\":\"highest-kills\"},{\"kind\":\"hero-slayer\",\"count\":42}]}";

	private static WorldFrame figures(String... entries) {
		StringBuilder json = new StringBuilder("{\"t\":\"figures\",\"figures\":[");
		for (int i = 0; i < entries.length; i++) {
			if (i > 0) {
				json.append(',');
			}
			json.append(entries[i]);
		}
		return WorldFrameCodec.decode(json.append("]}").toString());
	}

	@Test
	@DisplayName("decodes a figures frame into standing bodies")
	void decodesFigures() {
		WorldFrame frame = figures(FULL_FIGURE);

		assertThat(frame.kind).isEqualTo(WorldFrame.Kind.FIGURES);
		assertThat(frame.figures).hasSize(1);
		VillageFigure body = frame.figures.get(0);
		assertThat(body.post).isEqualTo(VillageFigure.Post.DEPTH);
		assertThat(body.depth).isEqualTo(5);
		assertThat(body.echoId).isEqualTo("5-177");
		assertThat(body.userName).isEqualTo("Somebody");
		assertThat(body.heroClass).isEqualTo("WARRIOR");
		assertThat(body.armorTier).isEqualTo(3);
		assertThat(body.lvl).isEqualTo(14);
		assertThat(body.hp).isEqualTo(30);
		assertThat(body.ht).isEqualTo(40);
		assertThat(body.killCount).isEqualTo(7);
		assertThat(body.timestamp).isEqualTo(1771000000000L);
	}

	@Test
	@DisplayName("reads a badge without a count as carrying none")
	void decodesBadgeCounts() {
		VillageFigure body = figures(FULL_FIGURE).figures.get(0);

		assertThat(body.badges).hasSize(2);
		assertThat(body.badges.get(0).kind).isEqualTo("highest-kills");
		assertThat(body.badges.get(0).hasCount()).isFalse();
		assertThat(body.badges.get(1).kind).isEqualTo("hero-slayer");
		assertThat(body.badges.get(1).count).isEqualTo(42);
	}

	@Test
	@DisplayName("keeps an unheld depth as an empty post so the regional boss can stand there")
	void keepsAnEmptyDepthPost() {
		WorldFrame frame = figures("{\"post\":\"depth\",\"depth\":15}");

		assertThat(frame.figures).hasSize(1);
		assertThat(frame.figures.get(0).isEmptyPost()).isTrue();
		assertThat(frame.figures.get(0).depth).isEqualTo(15);
	}

	@Test
	@DisplayName("reports an unknown armour tier rather than guessing at one")
	void leavesAnAbsentArmourTierUnknown() {
		VillageFigure body = figures("{\"post\":\"depth\",\"depth\":5,\"echo_id\":\"5-1\"}").figures.get(0);

		assertThat(body.armorTier).isEqualTo(VillageFigure.UNKNOWN_TIER);
	}

	@Test
	@DisplayName("drops a mention with no echo, since identity is the only way to inspect it")
	void dropsAnIdentitylessMention() {
		assertThat(figures("{\"post\":\"mention\",\"depth\":5}").figures).isEmpty();
	}

	@Test
	@DisplayName("drops a body whose post it does not recognise, keeping the rest of the frame")
	void dropsAnUnknownPost() {
		WorldFrame frame = figures("{\"post\":\"statue\",\"depth\":5,\"echo_id\":\"x\"}", FULL_FIGURE);

		assertThat(frame.figures).hasSize(1);
		assertThat(frame.figures.get(0).echoId).isEqualTo("5-177");
	}

	@Test
	@DisplayName("decodes an inspect bundle frame")
	void decodesEchoBundle() {
		WorldFrame frame = WorldFrameCodec.decode(
				"{\"t\":\"echo\",\"echo_id\":\"5-1\",\"echo_data_base64\":\"BUNDLE\"}");

		assertThat(frame.kind).isEqualTo(WorldFrame.Kind.ECHO);
		assertThat(frame.echoId).isEqualTo("5-1");
		assertThat(frame.echoData).isEqualTo("BUNDLE");
	}

	@Test
	@DisplayName("encodes a request for one figure's bundle")
	void encodesEchoRequest() throws Exception {
		JSONObject request = new JSONObject(WorldFrameCodec.encodeEchoReq("5-1"));

		assertThat(request.getString("t")).isEqualTo("echo_req");
		assertThat(request.getString("echo_id")).isEqualTo("5-1");
	}
}
