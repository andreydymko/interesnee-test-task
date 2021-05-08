package com.andreydymko.testtask.ui.animations

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator

/**
 * Object containing various static functions to animate [View]s.
 */
object AnimationCompat {
    /**
     * Will show [toShow] by gradually changing its alpha from 0 to 1,
     * and will *simultaneously* hide [toHide] by gradually changing its alpha from 1 to 0,
     */
    fun crossFade(toShow: View?, toHide: View?, duration: Long) {
        toShow?.apply {
            alpha = 0f
            visibility = View.VISIBLE

            animate().alpha(1f)
                .setDuration(duration)
                .setListener(null)
        }

        toHide?.let {
            it.animate()
                .alpha(1f)
                .setDuration(duration)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        it.visibility = View.GONE
                    }
                })
        }
    }

    /**
     * Will show [toShow] by sliding it down by [View.getHeight] using [DecelerateInterpolator].
     */
    // Fixme properly test this animation. View.height == 0?
    @Deprecated("Not working.", level = DeprecationLevel.ERROR)
    fun slideDownShowing(toShow: View?, duration: Long) {
        toShow?.let {
            it.visibility = View.VISIBLE
            it.animate()
                .translationY(it.height.toFloat())
                .setDuration(duration)
                .setInterpolator(DecelerateInterpolator())
                .setListener(null)
        }
    }

    /**
     * Will hide [toHide] by sliding it up by `-`[View.getBottom] using [AccelerateInterpolator].
     */
    // Fixme properly test this animation.
    @Deprecated("Not working.", level = DeprecationLevel.ERROR)
    fun slideUpHiding(toHide: View?, duration: Long) {
        toHide?.let {
            it.animate()
                .translationY(-(it.bottom.toFloat()))
                .setDuration(duration)
                .setInterpolator(AccelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        it.visibility = View.GONE
                    }
                })
        }
    }

    /**
     * Will show [toShow] by gradually changing its alpha from 0 to 1.
     */
    fun showByAlpha(toShow: View?, duration: Long) {
        toShow?.apply {
            alpha = 0f
            visibility = View.VISIBLE

            animate()
                .alpha(1f)
                .setDuration(duration)
                .setListener(null)
        }
    }

    /**
     * Will hide [toHide] by gradually changing its alpha from 1 to 0.
     */
    fun hideByAlpha(toHide: View?, duration: Long) {
        toHide?.let {
            it.animate()
                .alpha(0f)
                .setDuration(duration)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        it.visibility = View.GONE
                    }
                })
        }
    }
}