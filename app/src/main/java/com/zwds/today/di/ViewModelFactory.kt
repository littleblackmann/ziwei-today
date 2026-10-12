package com.zwds.today.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zwds.today.ui.family.FamilyEditViewModel
import com.zwds.today.ui.family.FamilyListViewModel
import com.zwds.today.ui.home.HomeViewModel
import com.zwds.today.ui.settings.SettingsViewModel
import com.zwds.today.ui.topic.TopicHistoryViewModel
import com.zwds.today.ui.topic.TopicPickerViewModel
import com.zwds.today.ui.topic.TopicResultViewModel

object ZwdsViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(HomeViewModel::class.java) ->
            HomeViewModel(ServiceLocator.familyRepository) as T

        modelClass.isAssignableFrom(FamilyListViewModel::class.java) ->
            FamilyListViewModel(ServiceLocator.familyRepository) as T

        modelClass.isAssignableFrom(FamilyEditViewModel::class.java) ->
            FamilyEditViewModel(
                ServiceLocator.familyRepository,
                ServiceLocator.userPreferences
            ) as T

        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(ServiceLocator.userPreferences, ServiceLocator.accountRepository) as T

        modelClass.isAssignableFrom(TopicPickerViewModel::class.java) ->
            TopicPickerViewModel(ServiceLocator.familyRepository) as T

        modelClass.isAssignableFrom(TopicResultViewModel::class.java) ->
            TopicResultViewModel(
                ServiceLocator.familyRepository,
                ServiceLocator.userPreferences,
                ServiceLocator.getTopicFortune
            ) as T

        modelClass.isAssignableFrom(TopicHistoryViewModel::class.java) ->
            TopicHistoryViewModel(
                ServiceLocator.familyRepository,
                ServiceLocator.topicHistoryRepository
            ) as T

        else -> throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}
