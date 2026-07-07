package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import java.util.HashMap;
import java.util.Map;

public final class EchoPolicyContext {

	private float selfHpRatio = 1f;
	private int distance;
	private String heroClass = "UNKNOWN";
	private boolean heroVisible;
	private final Map<String, Integer> itemCharges = new HashMap<>();

	public EchoPolicyContext selfHpRatio(float ratio) {
		this.selfHpRatio = ratio;
		return this;
	}

	public EchoPolicyContext distance(int distance) {
		this.distance = distance;
		return this;
	}

	public EchoPolicyContext heroClass(String heroClass) {
		this.heroClass = heroClass;
		return this;
	}

	public EchoPolicyContext heroVisible(boolean visible) {
		this.heroVisible = visible;
		return this;
	}

	public EchoPolicyContext hasItem(String item, int charges) {
		itemCharges.put(item, charges);
		return this;
	}

	public float selfHpRatio() {
		return selfHpRatio;
	}

	public int distance() {
		return distance;
	}

	public String heroClass() {
		return heroClass;
	}

	public boolean heroVisible() {
		return heroVisible;
	}

	public boolean hasItem(String item) {
		return itemCharges.containsKey(item) && itemCharges.get(item) > 0;
	}

	public int itemCharges(String item) {
		return itemCharges.getOrDefault(item, 0);
	}
}
