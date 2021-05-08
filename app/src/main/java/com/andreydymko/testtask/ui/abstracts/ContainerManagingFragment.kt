package com.andreydymko.testtask.ui.abstracts

import android.view.View
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment

/**
 * This [ToolbarManagingFragment] subclass will save, manipulate,
 * and later restore its container behaviour.
 *
 * For this fragment to function, variable [isManagingContainer] must be set to `true`.
 *
 * Containers behaviour will be saved [onStart] (only if it's not `null`),
 * cleared (set to `null`) in [onResume] and then restored to its original state in [onDetach].
 * That means, if this fragment was added to back stack, containers behaviour will still be null.
 *
 * You can manually restore containers behaviour by calling [restoreContainerNow].
 *
 * "Fragments container" - a [View] that this [Fragment] has been placed into.
 *
 * Example for determining fragments container:
 * ```
 * supportFragmentManager.commit {
 *      replace(R.id.fragment_container, ContainerManagingFragment())
 * }
 * ```
 * Where **`R.id.fragment_container`** is an id of fragments container view.
 *
 * As this fragment extending [ToolbarManagingFragment], it will also manage its activity
 * toolbar visibility. You can set [isManagingToolbar] to `false` to avoid it.
 * You can also restore both toolbar and containers behaviour by calling [restoreAllNow]
 * (this is an equivalent to calling [restoreContainerNow] and [restoreToolbarNow] subsequently).
 *
 * @see ToolbarManagingFragment
 * @see CoordinatorLayout.Behavior
 */
open class ContainerManagingFragment : ToolbarManagingFragment() {
    /**
     * Set this variable to `true` to allow this fragment to
     * manipulate behaviour of its fragment container.
     *
     * If set to `false`, fragment will still save fragment containers old behaviour,
     * but won't change it.
     *
     * *Default value is always `false`.*
     */
    var isManagingContainer = false

    private lateinit var fragmentContainer: FrameLayout

    private var oldBehavior: CoordinatorLayout.Behavior<View>? = null

    override fun onStart() {
        super.onStart()
        fragmentContainer = getFragmentContainer()
        // if fragment was added to back stack, this method will be invoked on pop back
        // so we don't need to re-save it's parent behaviour there
        if (getActualContainerBehaviour() != null) {
            saveContainerBehavior()
        }
    }

    private fun getFragmentContainer() : FrameLayout {
        return view?.parent as FrameLayout
    }

    private fun saveContainerBehavior() {
        oldBehavior = getLayoutParams()?.behavior
    }

    override fun onResume() {
        super.onResume()
        setContainerBehaviour(null)
    }

    override fun onDetach() {
        super.onDetach()
        setContainerBehaviour(oldBehavior)
    }

    /**
     * Call this function, to manually reset fragments container behaviour and toolbars visibility
     * to theirs original state.
     */
    fun restoreAllNow() {
        restoreContainerNow()
        super.restoreToolbarNow()
    }

    /**
     * Call this function, to manually reset fragments container behaviour to its original state.
     */
    fun restoreContainerNow() {
        setContainerBehaviour(oldBehavior)
    }

    private fun setContainerBehaviour(behavior: CoordinatorLayout.Behavior<View>?) {
        if (!isManagingContainer) return
        getLayoutParams()?.behavior = behavior
        fragmentContainer.requestLayout()
    }

    private fun getActualContainerBehaviour(): CoordinatorLayout.Behavior<View>? {
        return getLayoutParams()?.behavior
    }

    private fun getLayoutParams(): CoordinatorLayout.LayoutParams? {
        return fragmentContainer.layoutParams as? CoordinatorLayout.LayoutParams
    }
}