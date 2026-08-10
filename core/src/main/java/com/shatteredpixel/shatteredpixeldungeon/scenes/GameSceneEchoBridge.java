package com.shatteredpixel.shatteredpixeldungeon.scenes;

/**
 * Same-package access to {@link GameScene} scene state for Echo adapters.
 */
public final class GameSceneEchoBridge {

	private GameSceneEchoBridge() {
	}

	public static boolean hasScene() {
		return GameScene.scene != null;
	}
}
