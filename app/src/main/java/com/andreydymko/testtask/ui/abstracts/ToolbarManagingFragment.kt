package com.andreydymko.testtask.ui.abstracts

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

/**
 * This [Fragment] subclass will save, manipulate, and later restore its activity
 * toolbar visibility.
 *
 * For this fragment to function, variable [isManagingToolbar] must be set to `true`.
 *
 * Toolbars visibility will be saved in [onAttach], changed (hidden) in [onResume]
 * and then restored to its original state in [onDetach]. That means, if this fragment was added
 * to back stack, toolbar will still be hidden.
 *
 * You can manually restore toolbars visibility by calling [restoreToolbarNow].
 */
open class ToolbarManagingFragment : Fragment() {
    /**
     * Set this variable to `true` to allow this fragment to manipulate its activity toolbar.
     *
     * If set to `false`, fragment will still save old toolbars state, but won't change it.
     *
     * *Default value is always `false`.*
     */
    var isManagingToolbar = false

    private var wasToolbarVisible = true

    override fun onAttach(context: Context) {
        super.onAttach(context)
        saveToolbarVisibility()
    }

    private fun saveToolbarVisibility() {
        wasToolbarVisible = (activity as? AppCompatActivity)?.supportActionBar?.isShowing == true
    }


    override fun onResume() {
        super.onResume()
        hideToolbar()
    }

    private fun hideToolbar() {
        if (isManagingToolbar) {
            (activity as? AppCompatActivity)?.supportActionBar?.hide()
        }
    }

    /**
     * Call this function, to manually reset toolbars visibility to its original state.
     */
    fun restoreToolbarNow() {
        restoreToolbar()
    }

    override fun onDetach() {
        super.onDetach()
        restoreToolbar()
    }

    private fun restoreToolbar() {
        if (!isManagingToolbar) {
            return
        }
        val toolbar = (activity as? AppCompatActivity)?.supportActionBar
        if (wasToolbarVisible) {
            toolbar?.show()
        } else {
            toolbar?.hide()
        }
    }

}