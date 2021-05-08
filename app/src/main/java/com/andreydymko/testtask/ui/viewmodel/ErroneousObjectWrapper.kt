package com.andreydymko.testtask.ui.viewmodel

import java.lang.Exception

/**
 * Data class that contains [Exception] and object of type [T].
 *
 * Allows [androidx.lifecycle.MutableLiveData] to hold both
 * useful data in [someObject] and Exception in [exception] if an error occurred.
 */
data class ErroneousObjectWrapper<T>(var exception: Exception?, val someObject: T?)