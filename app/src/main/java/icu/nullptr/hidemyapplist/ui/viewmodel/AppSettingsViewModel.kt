package icu.nullptr.hidemyapplist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import icu.nullptr.hidemyapplist.common.JsonConfig

class AppSettingsViewModel(val state: State) : ViewModel() {

    class Factory(private val state: State) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppSettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AppSettingsViewModel(state) as T
            } else throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    class State(
        val packageName: String,
        var hideEnabled: Boolean,
        val config: JsonConfig.AppConfig
    )
}
