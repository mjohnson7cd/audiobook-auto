package io.github.mjohnson7cd.audiobook.features.car

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.mjohnson7cd.audiobook.application.Injector
import io.github.mjohnson7cd.audiobook.data.local.LibrarySyncRepository
import io.github.mjohnson7cd.audiobook.data.model.PlexLibrary
import io.github.mjohnson7cd.audiobook.data.model.ServerModel
import io.github.mjohnson7cd.audiobook.data.model.asServer
import io.github.mjohnson7cd.audiobook.data.sources.plex.IPlexLoginRepo
import io.github.mjohnson7cd.audiobook.data.sources.plex.IPlexLoginRepo.LoginState
import io.github.mjohnson7cd.audiobook.data.sources.plex.PlexConfig
import io.github.mjohnson7cd.audiobook.data.sources.plex.PlexLoginService
import io.github.mjohnson7cd.audiobook.data.sources.plex.PlexMediaService
import io.github.mjohnson7cd.audiobook.data.sources.plex.model.MediaType.Companion.ARTIST
import io.github.mjohnson7cd.audiobook.data.sources.plex.model.PlexUser
import io.github.mjohnson7cd.audiobook.data.sources.plex.model.asLibrary
import io.github.mjohnson7cd.audiobook.features.auth.PlexAuthCoordinator
import io.github.mjohnson7cd.audiobook.features.auth.PlexAuthState
import io.github.mjohnson7cd.audiobook.util.Event
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.HttpException
import timber.log.Timber

/**
 * Drives the in-car sign-in: plex.tv/link code → user → server → library.
 *
 * The step shown is derived from [IPlexLoginRepo.loginEvent], the same source of truth the phone
 * onboarding uses, so a partially completed sign-in resumes at the right step and the
 * MediaPlayerService sees [LoginState.LOGGED_IN_FULLY] as soon as we finish.
 * Single users / servers / libraries are chosen automatically to minimise taps in the car.
 */
class CarSignInViewModel(
    private val plexLoginRepo: IPlexLoginRepo,
    private val plexLoginService: PlexLoginService,
    private val plexMediaService: PlexMediaService,
    private val plexConfig: PlexConfig,
    private val librarySyncRepository: LibrarySyncRepository,
) : ViewModel() {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val component = Injector.get()
            return CarSignInViewModel(
                component.plexLoginRepo(),
                component.plexLoginService(),
                component.plexMediaService(),
                component.plexConfig(),
                component.librarySyncRepo(),
            ) as T
        }
    }

    sealed class UiState {
        data class Loading(val message: Message) : UiState()

        data class ShowCode(val code: String) : UiState()

        data class ChooseUser(val users: List<PlexUser>) : UiState()

        data class EnterUserPin(val user: PlexUser, val wrongPin: Boolean = false) : UiState()

        data class ChooseServer(val servers: List<ServerModel>) : UiState()

        data class ChooseLibrary(val libraries: List<PlexLibrary>) : UiState()

        object Done : UiState()

        object CodeExpired : UiState()

        data class Error(val message: Message, val detail: String? = null) : UiState()
    }

    /** Resource-free message keys; the activity maps them to strings. */
    enum class Message {
        REQUESTING_CODE,
        LOADING,
        CONNECTING,
        NO_SERVERS,
        NO_LIBRARIES,
        NETWORK,
    }

    private val _state = MutableLiveData<UiState>(UiState.Loading(Message.LOADING))
    val state: LiveData<UiState> get() = _state

    /** Name of the server being connected to, for the CONNECTING message. */
    var connectingServerName: String = ""
        private set

    private var authCoordinator: PlexAuthCoordinator? = null
    private var authJob: Job? = null
    private var lastRoutedLoginState: LoginState? = null
    private var awaitingLibraries = false

    private val loginObserver =
        Observer<Event<LoginState>> { event -> route(event.peekContent()) }

    private val connectionObserver =
        Observer<PlexConfig.ConnectionState> { connection ->
            if (!awaitingLibraries) return@Observer
            when (connection) {
                PlexConfig.ConnectionState.CONNECTED -> loadLibraries()
                PlexConfig.ConnectionState.CONNECTION_FAILED -> {
                    awaitingLibraries = false
                    _state.postValue(UiState.Error(Message.NETWORK, connectingServerName))
                }
                else -> Unit
            }
        }

    init {
        plexLoginRepo.loginEvent.observeForever(loginObserver)
        plexConfig.connectionState.observeForever(connectionObserver)
        plexLoginRepo.determineLoginState()
    }

    private fun route(loginState: LoginState) {
        if (loginState == lastRoutedLoginState) return
        Timber.i("[CarSignIn] login state -> $loginState")
        when (loginState) {
            LoginState.NOT_LOGGED_IN -> startCodeFlow()
            LoginState.AWAITING_LOGIN_RESULTS -> return // transient: keep current UI, don't latch
            LoginState.FAILED_TO_LOG_IN -> _state.postValue(UiState.Error(Message.NETWORK))
            LoginState.LOGGED_IN_NO_USER_CHOSEN -> loadUsers()
            LoginState.LOGGED_IN_NO_SERVER_CHOSEN -> loadServers()
            LoginState.LOGGED_IN_NO_LIBRARY_CHOSEN -> connectForLibraries()
            LoginState.LOGGED_IN_FULLY -> finishSignIn()
        }
        lastRoutedLoginState = loginState
    }

    /** Retry whatever step failed (or request a fresh code). */
    fun retry() {
        lastRoutedLoginState = null
        authCoordinator?.reset()
        plexLoginRepo.determineLoginState()
    }

    fun requestNewCode() {
        lastRoutedLoginState = null
        startCodeFlow()
    }

    private fun startCodeFlow() {
        authJob?.cancel()
        authCoordinator?.dispose()
        _state.postValue(UiState.Loading(Message.REQUESTING_CODE))
        val coordinator =
            PlexAuthCoordinator(
                plexLoginRepo,
                viewModelScope,
                PlexAuthCoordinator.LINK_CODE_TIMEOUT_MS,
                useLinkCode = true,
            )
        authCoordinator = coordinator
        authJob =
            viewModelScope.launch {
                coordinator.startAuth().collect { authState ->
                    when (authState) {
                        is PlexAuthState.WaitingForUser -> _state.postValue(UiState.ShowCode(authState.pinCode))
                        is PlexAuthState.Polling ->
                            if (_state.value !is UiState.ShowCode && authState.pinCode.isNotEmpty()) {
                                _state.postValue(UiState.ShowCode(authState.pinCode))
                            }
                        // Success: loginEvent advances (user/server step) and route() takes over
                        is PlexAuthState.Success -> Unit
                        is PlexAuthState.Timeout -> _state.postValue(UiState.CodeExpired)
                        is PlexAuthState.Error -> _state.postValue(UiState.Error(Message.NETWORK, authState.message))
                        else -> Unit
                    }
                }
            }
    }

    private fun loadUsers() {
        _state.postValue(UiState.Loading(Message.LOADING))
        viewModelScope.launch {
            try {
                val users = plexLoginService.getUsersForAccount().users
                when {
                    users.isEmpty() -> _state.postValue(UiState.Error(Message.NETWORK, "No Plex users"))
                    users.size == 1 -> pickUser(users.first())
                    else -> _state.postValue(UiState.ChooseUser(users))
                }
            } catch (t: Throwable) {
                Timber.e(t, "[CarSignIn] failed to load users")
                _state.postValue(UiState.Error(Message.NETWORK, t.message))
            }
        }
    }

    fun pickUser(user: PlexUser) {
        if (user.hasPassword) {
            _state.postValue(UiState.EnterUserPin(user))
        } else {
            submitUserPin(user, null)
        }
    }

    fun submitUserPin(
        user: PlexUser,
        pin: String?,
    ) {
        _state.postValue(UiState.Loading(Message.LOADING))
        viewModelScope.launch {
            try {
                val responseUser = plexLoginService.pickUser(user.uuid, pin)
                check(!responseUser.authToken.isNullOrEmpty()) { "No auth token received for user" }
                plexLoginRepo.chooseUser(responseUser)
            } catch (e: HttpException) {
                Timber.w("[CarSignIn] pick user failed: ${e.code()}")
                if (e.code() == 403) {
                    _state.postValue(UiState.EnterUserPin(user, wrongPin = true))
                } else {
                    _state.postValue(UiState.Error(Message.NETWORK, "HTTP ${e.code()}"))
                }
            } catch (t: Throwable) {
                Timber.e(t, "[CarSignIn] pick user failed")
                _state.postValue(UiState.Error(Message.NETWORK, t.message))
            }
        }
    }

    private fun loadServers() {
        _state.postValue(UiState.Loading(Message.LOADING))
        viewModelScope.launch {
            try {
                val servers =
                    plexLoginService.resources()
                        .filter { it.provides.contains("server") }
                        .map { it.asServer() }
                when (servers.size) {
                    0 -> _state.postValue(UiState.Error(Message.NO_SERVERS))
                    1 -> chooseServer(servers.first())
                    else -> _state.postValue(UiState.ChooseServer(servers))
                }
            } catch (t: Throwable) {
                Timber.e(t, "[CarSignIn] failed to load servers")
                _state.postValue(UiState.Error(Message.NETWORK, t.message))
            }
        }
    }

    fun chooseServer(server: ServerModel) {
        connectingServerName = server.name
        plexLoginRepo.chooseServer(server)
    }

    @OptIn(InternalCoroutinesApi::class) // PlexConfig.connectToServerWithRetryAndState, as in ChooseLibraryViewModel
    private fun connectForLibraries() {
        connectingServerName = Injector.get().plexPrefs().server?.name.orEmpty()
        _state.postValue(UiState.Loading(Message.CONNECTING))
        awaitingLibraries = true
        // Retry-enabled connect: the car is almost never on the server's LAN, so WAN/relay
        // connections need the retry path (same as ChooseLibraryViewModel).
        // It always posts CONNECTING then CONNECTED/CONNECTION_FAILED; connectionObserver reacts.
        plexConfig.connectToServerWithRetryAndState(plexMediaService)
    }

    private fun loadLibraries() {
        awaitingLibraries = false
        viewModelScope.launch {
            try {
                val libraries =
                    plexMediaService.retrieveLibraries()
                        .plexMediaContainer.plexDirectories
                        .filter { it.type == ARTIST.typeString }
                        .map { it.asLibrary() }
                when (libraries.size) {
                    0 -> _state.postValue(UiState.Error(Message.NO_LIBRARIES))
                    1 -> chooseLibrary(libraries.first())
                    else -> _state.postValue(UiState.ChooseLibrary(libraries))
                }
            } catch (t: Throwable) {
                Timber.e(t, "[CarSignIn] failed to load libraries")
                _state.postValue(UiState.Error(Message.NETWORK, t.message))
            }
        }
    }

    fun chooseLibrary(library: PlexLibrary) {
        _state.postValue(UiState.Loading(Message.LOADING))
        plexLoginRepo.chooseLibrary(library)
    }

    private fun finishSignIn() {
        authCoordinator?.dispose()
        // There is no home screen in the car to kick off the first sync, so do it here.
        librarySyncRepository.refreshLibrary()
        _state.postValue(UiState.Done)
    }

    fun cancel() {
        authCoordinator?.cancelAuth()
    }

    override fun onCleared() {
        plexLoginRepo.loginEvent.removeObserver(loginObserver)
        plexConfig.connectionState.removeObserver(connectionObserver)
        authCoordinator?.dispose()
        super.onCleared()
    }
}
