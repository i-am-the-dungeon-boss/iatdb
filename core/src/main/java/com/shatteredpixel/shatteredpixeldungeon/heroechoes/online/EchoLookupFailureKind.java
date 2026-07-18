package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

/**
 * Why a ranked/local echo lookup failed (for UI hints).
 */
public enum EchoLookupFailureKind {
	/** Transport threw (timeout, connection refused, etc.). */
	NETWORK,
	/** HTTP response was an error status other than 404. */
	SERVER,
	/** 200 body could not be decoded. */
	DECODE,
	/** Ranked online sync is not configured / available. */
	UNAVAILABLE,
	/** Unexpected failure. */
	UNKNOWN
}
