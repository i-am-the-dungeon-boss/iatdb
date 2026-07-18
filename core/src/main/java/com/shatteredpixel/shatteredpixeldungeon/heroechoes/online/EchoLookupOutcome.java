package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndEchoFetchFailed;

/**
 * Result of looking up an echo for a boss depth: success, empty pool, or fetch
 * failure.
 */
public final class EchoLookupOutcome {

	public final EchoLookupStatus status;
	public final EchoFetchResult result;
	public final EchoLookupFailureKind failureKind;
	public final int httpStatus;

	private EchoLookupOutcome(
			EchoLookupStatus status,
			EchoFetchResult result,
			EchoLookupFailureKind failureKind,
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
		return new EchoLookupOutcome(EchoLookupStatus.FOUND, result, null, -1);
	}

	public static EchoLookupOutcome notFound() {
		return new EchoLookupOutcome(EchoLookupStatus.NOT_FOUND, null, null, -1);
	}

	public static EchoLookupOutcome error() {
		return error(EchoLookupFailureKind.UNKNOWN, -1);
	}

	public static EchoLookupOutcome error(EchoLookupFailureKind kind) {
		return error(kind, -1);
	}

	public static EchoLookupOutcome error(EchoLookupFailureKind kind, int httpStatus) {
		if (kind == null) {
			kind = EchoLookupFailureKind.UNKNOWN;
		}
		return new EchoLookupOutcome(EchoLookupStatus.ERROR, null, kind, httpStatus);
	}

	public boolean isFound() {
		return status == EchoLookupStatus.FOUND;
	}

	public boolean isError() {
		return status == EchoLookupStatus.ERROR;
	}

	public boolean isNotFound() {
		return status == EchoLookupStatus.NOT_FOUND;
	}

	/** Localized one-line hint for dialogs; empty when not an ERROR. */
	public String failureHint() {
		if (!isError()) {
			return "";
		}
		EchoLookupFailureKind kind = failureKind != null ? failureKind : EchoLookupFailureKind.UNKNOWN;
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
