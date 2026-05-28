package com.reelgrab.feature.settings.disclaimer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelgrab.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Marks the first-launch disclaimer as accepted.
 *
 * Why a dedicated ViewModel? Persisting the flag is the only mutation this screen performs;
 * keeping the call in a ViewModel (rather than the Composable) means it survives recomposition
 * and config change without ever firing twice.
 */
@HiltViewModel
class DisclaimerViewModel @Inject constructor(
    private val updateSettings: UpdateSettingsUseCase,
) : ViewModel() {

    fun onAccept(onComplete: () -> Unit) {
        viewModelScope.launch {
            updateSettings.setDisclaimerAccepted(true)
            onComplete()
        }
    }
}
