package com.runiverse.app.run.common

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 서버 설정이 아니라 워치에만 저장한다. */
class ComboEffectSetting(private val preferences: SharedPreferences) {
    private val _isOn = MutableStateFlow(preferences.getBoolean(KEY_IS_ON, true))
    val isOn: StateFlow<Boolean> = _isOn.asStateFlow()

    fun set(isOn: Boolean) {
        _isOn.value = isOn
        preferences.edit { putBoolean(KEY_IS_ON, isOn) }
    }

    private companion object {
        const val KEY_IS_ON = "combo_effect_on"
    }
}
