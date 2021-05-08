package com.andreydymko.testtask.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.andreydymko.testtask.R


/**
 * A simple [Fragment] subclass.
 * Contains simple text suggestion for user to "find a lovely picture"
 * Use the [SuggestSearchFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SuggestSearchFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_suggest_search, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment
         * @return A new instance of [SuggestSearchFragment].
         */
        @JvmStatic
        fun newInstance() = SuggestSearchFragment()
    }
}