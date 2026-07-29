package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndEchoFetchFailed;

/**
 * Result of looking up an echo for a boss depth: success, empty pool, or fetch
 * failure.
 */
public final class EchoLookupOutcome {

	public enum Status {
		FOUND,
		NOT_FOUND,
		ERROR
	}

	/** Why a ranked/local echo lookup failed (for UI hints). */
	public enum FailureKind {
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

	public final Status status;
	public final EchoFetchResult result;
	public final FailureKind failureKind;
	public final int httpStatus;

	private EchoLookupOutcome(
			Status status,
			EchoFetchResult result,
			FailureKind failureKind,
			int httpStatus) {
		this.status = status;
		this.result = result;
		this.failureKind = failureKind;
		this.httpStatus = httpStatus;
	}

	public static EchoLookupOutcome found(EchoFetchResult result) {
		if (result == null) {
			throw new IllegalArgumentException("result is required for FOUND");
		}
		return new EchoLookupOutcome(Status.FOUND, result, null, -1);
	}

	public static EchoLookupOutcome notFound() {
		return new EchoLookupOutcome(Status.NOT_FOUND, null, null, -1);
	}

	public static EchoLookupOutcome error() {
		return error(FailureKind.UNKNOWN, -1);
	}

	public static EchoLookupOutcome error(FailureKind kind) {
		return error(kind, -1);
	}

	public static EchoLookupOutcome error(FailureKind kind, int httpStatus) {
		if (kind == null) {
			kind = FailureKind.UNKNOWN;
		}
		return new EchoLookupOutcome(Status.ERROR, null, kind, httpStatus);
	}

	public boolean isFound() {
		return status == Status.FOUND;
	}

	public boolean isError() {
		return status == Status.ERROR;
	}

	public boolean isNotFound() {
		return status == Status.NOT_FOUND;
	}

	/** Localized one-line hint for dialogs; empty when not an ERROR. */
	public String failureHint() {
		if (!isError()) {
			return "";
		}
		FailureKind kind = failureKind != null ? failureKind : FailureKind.UNKNOWN;
		switch (kind) {
			case NETWORK:
				return Messages.get(WndEchoFetchFailed.class, "reason_network");
			case SERVER:
				if (httpStatus > 0) {
					return Messages.get(WndEchoFetchFailed.class, "reason_server", httpStatus);
				}
				return Messages.get(WndEchoFetchFailed.class, "reason_server_unknown");
			case DECODE:
				return Messages.get(WndEchoFetchFailed.class, "reason_decode");
			case UNAVAILABLE:
				return Messages.get(WndEchoFetchFailed.class, "reason_unavailable");
			case UNKNOWN:
			default:
				return Messages.get(WndEchoFetchFailed.class, "reason_unknown");
		}
	}
}
