package com.andreydymko.testtask.ui.abstracts

import android.view.View
import android.view.Window
import android.view.WindowManager

/**
 * This [ContainerManagingFragment] subclass will save, manipulate,
 * and later restore its [Window] flags.
 *
 * Flags will be saved and set in [onResume], and then restored to its original state in [onPause].
 *
 * This fragment will set its window flags to such, that window (to which fragment is attached)
 * will hide navigation and enter fullscreen mode. Please see [flags] to understand what kind of
 * flags will be set.
 *
 * **Please note that this class extends [ContainerManagingFragment], so try to not to get caught
 * in situation in which behaviour of this fragment looks undefined.**
 *
 * @see ContainerManagingFragment
 * @see ToolbarManagingFragment
 */
open class FullscreenContainerManagingFragment : ContainerManagingFragment() {
    /**
     * Flags that will be set to [Window] in [onResume] and then removed in [onPause].
     */
    protected open var flags =
        View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_FULLSCREEN

    private var oldFlags: Int = 0

    override fun onResume() {
        super.onResume()
        val window = getWindow()
        oldFlags = window?.attributes?.flags ?: 0

        window?.clearFlags(0xFFFFFFFF.toInt())
        window?.addFlags(flags)
    }

    override fun onPause() {
        super.onPause()
        val window = getWindow()
        window?.clearFlags(flags)
        window?.addFlags(oldFlags)
    }

    private fun getWindow(): Window? {
        return activity?.window
    }
}