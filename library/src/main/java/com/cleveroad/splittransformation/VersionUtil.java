package com.cleveroad.splittransformation;

import android.content.Context;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;

/**
 * Util that dealing with Android versions methods.
 */
class VersionUtil {
	private VersionUtil() {

	}

    /**
     * Get color from resources.
     * @param context instance of context
     * @param colorId color resource id
     * @return color int
     */
    @ColorInt
	@SuppressWarnings("deprecation")
	public static int color(@NonNull Context context, @ColorRes int colorId) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return context.getColor(colorId);
		}
		return context.getResources().getColor(colorId);
	}
}
