package com.verity.tool.util;

import android.content.Context;
import android.util.TypedValue;

public final class ThemeUtils {

    private ThemeUtils() {
    }

    public static int color(Context context, int attrRes) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attrRes, value, true);
        return value.data;
    }
}
