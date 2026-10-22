package io.github.mjohnson7cd.audiobook.features.settings

import androidx.databinding.BindingAdapter

@BindingAdapter("preferences")
fun setPreferencesForList(
    settingsList: SettingsList,
    prefs: List<PreferenceModel>,
) {
    settingsList.setPreferences(prefs)
}
