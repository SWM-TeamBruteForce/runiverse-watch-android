package com.runiverse.app.run

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// TODO: 콤보 효과 — 효과를 만들 때 이 값으로 큰 순간 터짐을 켜고 끈다
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
