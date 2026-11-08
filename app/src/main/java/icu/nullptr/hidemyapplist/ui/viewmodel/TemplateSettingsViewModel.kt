package icu.nullptr.hidemyapplist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.fragment.TemplateSettingsFragmentArgs
import kotlinx.coroutines.flow.MutableStateFlow

class TemplateSettingsViewModel(
    val originalName: String?,
    val isWhitelist: Boolean,
    var name: String?
) : ViewModel() {

    class Factory(private val args: TemplateSettingsFragmentArgs) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TemplateSettingsViewModel::class.java)) {
                val viewModel = TemplateSettingsViewModel(args.name, args.isWhitelist, args.name)
                args.name?.let {
                    val appliedPackages = ConfigManager.getTemplateAppliedPackages(it)
                    val templatePackages = ConfigManager.getTemplatePackages(it)
                    viewModel.appliedPackages.value = appliedPackages
                    viewModel.templatePackages.value = templatePackages
                    viewModel.originalAppliedPackages.addAll(appliedPackages)
                    viewModel.originalTemplatePackages.addAll(templatePackages)
                }
                @Suppress("UNCHECKED_CAST")
                return viewModel as T
            } else throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    val appliedPackages = MutableStateFlow<ArrayList<String>>(ArrayList())
    val templatePackages = MutableStateFlow<ArrayList<String>>(ArrayList())
    private val originalAppliedPackages = ArrayList<String>()
    private val originalTemplatePackages = ArrayList<String>()

    fun hasUnsavedChanges(): Boolean {
        return name?.trim().orEmpty() != originalName.orEmpty() ||
                appliedPackages.value.toSet() != originalAppliedPackages.toSet() ||
                templatePackages.value.toSet() != originalTemplatePackages.toSet()
    }
}
