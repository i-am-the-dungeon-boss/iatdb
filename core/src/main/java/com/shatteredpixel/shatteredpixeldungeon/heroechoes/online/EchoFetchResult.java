package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicy;

/** Spawnable echo + policy pair from lookup/fetch. */
public final class EchoFetchResult {

	public final Echo echo;
	public final EchoPolicy policy;

	public EchoFetchResult(Echo echo, EchoPolicy policy) {
		this.echo = echo;
		this.policy = policy;
	}
}
