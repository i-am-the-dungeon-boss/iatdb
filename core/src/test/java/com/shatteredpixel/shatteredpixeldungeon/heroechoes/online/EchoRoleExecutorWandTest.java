package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoRoleExecutorWandTest {

	@Test
	@DisplayName("AI wand charge spend decrements without hero spendAndNext")
	void aiWandChargeSpendDecrementsOnly() {
		WandOfMagicMissile wand = new WandOfMagicMissile();
		wand.curCharges = 3;
		wand.maxCharges = 3;

		wand.spendChargesForAi();

		Assertions.assertThat(wand.curCharges).isEqualTo(2);
	}
}
