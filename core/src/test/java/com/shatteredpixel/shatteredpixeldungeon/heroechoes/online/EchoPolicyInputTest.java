package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

@ExtendWith(GdxTestExtension.class)
class EchoPolicyInputTest {

	@Test
	@DisplayName("fromEcho rejects null echo")
	void fromEchoRejectsNullEcho() {
		Assertions.assertThatThrownBy(() -> EchoPolicyInput.fromEcho(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("echo");
	}

	@Test
	@DisplayName("fromEcho rejects echo that cannot restore a hero")
	void fromEchoRejectsUnrestorableEcho() {
		Echo echo = EchoTestSupport.warriorEcho(5);

		Assertions.assertThatThrownBy(() -> EchoPolicyInput.fromEcho(echo))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("restor");
	}

	@Test
	@DisplayName("fromEcho builds policy input from restored hero")
	void fromEchoBuildsFromRestoredHero() {
		Echo echo = EchoTestSupport.warriorEchoWithData(5);

		EchoPolicyInput input = EchoPolicyInput.fromEcho(echo);

		Assertions.assertThat(input.heroClass).isEqualTo("WARRIOR");
		Assertions.assertThat(input.lvl).isEqualTo(6);
		Assertions.assertThat(input.items).isNotEmpty();
	}

	@Test
	@DisplayName("constructor rejects blank hero class")
	void constructorRejectsBlankHeroClass() {
		Assertions.assertThatThrownBy(
				() -> new EchoPolicyInput("", "NONE", null, 1, List.of(), Map.of()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("hero_class");
	}

	@Test
	@DisplayName("fromHero rejects null hero")
	void fromHeroRejectsNullHero() {
		Assertions.assertThatThrownBy(() -> EchoPolicyInput.fromHero(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("hero");
	}

	@Test
	@DisplayName("fromHero rejects hero without hero class")
	void fromHeroRejectsMissingHeroClass() {
		Hero hero = new Hero();
		hero.heroClass = null;

		Assertions.assertThatThrownBy(() -> EchoPolicyInput.fromHero(hero))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("hero_class");
	}
}
