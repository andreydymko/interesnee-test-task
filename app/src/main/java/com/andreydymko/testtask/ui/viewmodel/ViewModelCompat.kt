package com.andreydymko.testtask.ui.viewmodel

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.andreydymko.testtask.R
import com.google.android.material.snackbar.Snackbar

/**
 * Various functions that may help you to work with [ImageListViewModel].
 */
object ViewModelCompat {

    /**
     * Make a [Snackbar] with empty text, slightly transparent background
     * and action with text [R.string.action_retry]
     * that will call [ImageListViewModel.loadPageBlocking] on click.
     *
     * This function will not call [Snackbar.show].
     *
     * @param context The context to use to create the Snackbar view.
     * @param view The view to find a parent from. This view is also used to find the anchor view when
     *     calling {@link Snackbar#setAnchorView(int)}.
     * @param viewModel instance of [ImageListViewModel].
     * @return [Snackbar] with empty text and "Retry" action.
     */
    fun setupRetrySnackbar(
        context: Context,
        view: View,
        viewModel: ImageListViewModel
    ): Snackbar {
        return Snackbar.make(
            context,
            view,
            "",
            Snackbar.LENGTH_INDEFINITE
        ).setAction(context.getString(R.string.action_retry)) {
            viewModel.loadPageBlocking()
        }.apply {
            this.view.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.snackbar_90_transparent
                )
            )
        }
    }
}