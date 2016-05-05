package com.cleveroad.splittransformation;

import android.support.annotation.NonNull;
import android.view.View;

/**
 * Helper interface that allows to check if view is complex or not.
 */
public interface ComplexViewDetector {

    /**
     * Check if view is a complex view.
     *
     * @param view some view
     * @return true if view is complex, false otherwise
     */
    boolean isComplexView(@NonNull View view);
}
