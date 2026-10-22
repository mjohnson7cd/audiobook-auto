package io.github.mjohnson7cd.audiobook.injection.components

import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dagger.Component
import io.github.mjohnson7cd.audiobook.application.MainActivity
import io.github.mjohnson7cd.audiobook.application.MainActivityViewModel
import io.github.mjohnson7cd.audiobook.features.account.AccountListFragment
import io.github.mjohnson7cd.audiobook.features.account.AccountListViewModel
import io.github.mjohnson7cd.audiobook.features.account.LibrarySelectorBottomSheet
import io.github.mjohnson7cd.audiobook.features.bookdetails.AudiobookDetailsFragment
import io.github.mjohnson7cd.audiobook.features.bookdetails.AudiobookDetailsViewModel
import io.github.mjohnson7cd.audiobook.features.collections.CollectionDetailsFragment
import io.github.mjohnson7cd.audiobook.features.collections.CollectionsFragment
import io.github.mjohnson7cd.audiobook.features.currentlyplaying.CurrentlyPlayingFragment
import io.github.mjohnson7cd.audiobook.features.currentlyplaying.CurrentlyPlayingViewModel
import io.github.mjohnson7cd.audiobook.features.home.HomeFragment
import io.github.mjohnson7cd.audiobook.features.library.LibraryFragment
import io.github.mjohnson7cd.audiobook.features.player.MediaServiceConnection
import io.github.mjohnson7cd.audiobook.features.player.ProgressUpdater
import io.github.mjohnson7cd.audiobook.features.settings.DebugInfoDialogFragment
import io.github.mjohnson7cd.audiobook.features.settings.DebugInfoViewModel
import io.github.mjohnson7cd.audiobook.features.settings.SettingsFragment
import io.github.mjohnson7cd.audiobook.features.settings.SettingsViewModel
import io.github.mjohnson7cd.audiobook.injection.modules.ActivityModule
import io.github.mjohnson7cd.audiobook.injection.scopes.ActivityScope
import io.github.mjohnson7cd.audiobook.navigation.Navigator
import io.github.mjohnson7cd.audiobook.views.ModalBottomSheetSpeedChooser

@ActivityScope
@Component(dependencies = [AppComponent::class], modules = [ActivityModule::class])
interface ActivityComponent {
    fun navigator(): Navigator

    fun progressUpdater(): ProgressUpdater

    fun localBroadcastManager(): LocalBroadcastManager

    fun mediaServiceConnection(): MediaServiceConnection

    fun mainActivityViewModelFactory(): MainActivityViewModel.Factory

    fun currentPlayingViewModelFactory(): CurrentlyPlayingViewModel.Factory

    fun audiobookDetailsViewModelFactory(): AudiobookDetailsViewModel.Factory

    fun settingsViewModelFactory(): SettingsViewModel.Factory

    fun debugInfoViewModelFactory(): DebugInfoViewModel.Factory

    fun accountListViewModelFactory(): AccountListViewModel.Factory

    fun inject(activity: MainActivity)

    fun inject(accountListFragment: AccountListFragment)

    fun inject(librarySelectorBottomSheet: LibrarySelectorBottomSheet)

    fun inject(libraryFragment: LibraryFragment)

    fun inject(detailsFragment: AudiobookDetailsFragment)

    fun inject(homeFragment: HomeFragment)

    fun inject(settingsFragment: SettingsFragment)

    fun inject(collectionsFragment: CollectionsFragment)

    fun inject(collectionDetailsFragment: CollectionDetailsFragment)

    fun inject(currentlyPlayingFragment: CurrentlyPlayingFragment)

    fun inject(modalBottomSheetSpeedChooser: ModalBottomSheetSpeedChooser)

    fun inject(debugInfoDialogFragment: DebugInfoDialogFragment)
}
