/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * I am the Dungeon Boss
 * Copyright (C) 2026 Dungeon Boss
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.services.updates;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.watabou.utils.Callback;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class Updates {

	public static UpdateService service;

	public static boolean supportsUpdates() {
		return service != null;
	}

	private static Date lastCheck = null;
	private static final long CHECK_DELAY = 1000 * 60 * 60; // 1 hour

	public static boolean supportsUpdatePrompts() {
		return supportsUpdates() && service.supportsUpdatePrompts();
	}

	public static boolean supportsBetaChannel() {
		return supportsUpdates() && service.supportsBetaChannel();
	}

	public static void checkForUpdate() {
		if (!supportsUpdatePrompts())
			return;
		if (lastCheck != null && (new Date().getTime() - lastCheck.getTime()) < CHECK_DELAY)
			return;

		// We do this so that automatically enabled beta checking (for users who DLed a
		// beta) persists afterward
		if (SPDSettings.betas()) {
			SPDSettings.betas(true);
		}

		service.checkForUpdate(!SPDSettings.WiFi(), SPDSettings.betas(), new UpdateService.UpdateResultCallback() {
			@Override
			public void onUpdateAvailable(AvailableUpdateData update) {
				lastCheck = new Date();
				updateData = update;
			}

			@Override
			public void onNoUpdateFound() {
				lastCheck = new Date();
			}

			@Override
			public void onConnectionFailed() {
				lastCheck = null;
			}
		});
	}

	/**
	 * Blocking game-version check that ignores the hourly cache. Used before echo
	 * boss prefetch so outdated clients cannot enter a boss floor. Returns the
	 * update payload when required, otherwise null (including connection failure).
	 */
	public static AvailableUpdateData checkForUpdateImmediate() {
		if (!supportsUpdatePrompts())
			return null;

		if (SPDSettings.betas()) {
			SPDSettings.betas(true);
		}

		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<AvailableUpdateData> result = new AtomicReference<>();
		service.checkForUpdate(true, SPDSettings.betas(), new UpdateService.UpdateResultCallback() {
			@Override
			public void onUpdateAvailable(AvailableUpdateData update) {
				lastCheck = new Date();
				updateData = update;
				result.set(update);
				latch.countDown();
			}

			@Override
			public void onNoUpdateFound() {
				lastCheck = new Date();
				updateData = null;
				latch.countDown();
			}

			@Override
			public void onConnectionFailed() {
				lastCheck = null;
				latch.countDown();
			}
		});
		try {
			latch.await();
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
		return result.get();
	}

	public static void launchUpdate(AvailableUpdateData data) {
		service.initializeUpdate(data);
	}

	private static AvailableUpdateData updateData = null;

	public static boolean updateAvailable() {
		return updateData != null;
	}

	public static AvailableUpdateData updateData() {
		return updateData;
	}

	public static void clearUpdate() {
		updateData = null;
		lastCheck = null;
	}

	public static boolean supportsReviews() {
		return supportsUpdates() && service.supportsReviews();
	}

	public static void launchReview(Callback callback) {
		if (supportsUpdates()) {
			service.initializeReview(new UpdateService.ReviewResultCallback() {
				@Override
				public void onComplete() {
					callback.call();
				}
			});
		} else {
			callback.call();
		}
	}

	public static void openReviewURI() {
		if (supportsUpdates()) {
			service.openReviewURI();
		}
	}

}
