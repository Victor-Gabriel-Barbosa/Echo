package com.example.mapa.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mapa.data.remote.datasource.AuthRemote
import com.example.mapa.data.remote.dto.LocationDTO
import com.example.mapa.data.repository.LocationRepository
import com.example.mapa.model.LocationUiState
import com.example.mapa.util.GeofenceManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel para gerenciar locais salvos.
 *
 * @property locationRepo O repositório para operações de locais.
 * @property authRemote O repositório para operações de autenticação.
 */
class LocationViewModel(
    private val locationRepo: LocationRepository,
    authRemote: AuthRemote,
    private val geofenceManager: GeofenceManager
) : ViewModel() {
    /**
     * Estado de carregamento.
     */
    private val _loading = MutableStateFlow(false)

    /**
     * Canal para enviar mensagens de eventos para a UI.
     */
    private val _channel = Channel<String>(Channel.BUFFERED)
    val channel = _channel.receiveAsFlow()

    /**
     * Fluxo de todos os locais.
     */
    private val locationFlow = locationRepo.getLocations()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            replay = 1
        )

    /**
     * Fluxo do usuário autenticado.
     */
    private val sharedUserFlow = authRemote.user
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            replay = 1
        )

    /**
     * Fluxo de locais do usuário.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val locationUserFlow = sharedUserFlow
        .flatMapLatest { user ->
            if (user?.uid.isNullOrBlank()) flowOf(emptyList())
            else locationRepo.getLocationsUser(user.uid)
        }

    /**
     * O estado da UI para a tela de locais.
     */
    val uiState: StateFlow<LocationUiState> = combine(
        locationFlow,
        locationUserFlow,
        _loading
    ) { locations, userLocations, loading ->
        LocationUiState(
            locations = locations,
            locationsUser = userLocations,
            loading = loading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LocationUiState(loading = true)
    )

    init {
        // Observa mudanças nos locais e atualiza os geofences automaticamente
        viewModelScope.launch {
            combine(locationFlow, sharedUserFlow) { locations, user ->
                Pair(locations, user?.uid)
            }.collectLatest { (locations, uid) ->
                if (uid != null && locations.isNotEmpty()) {
                    try {
                        geofenceManager.setupGeofences(locations, uid)
                    } catch (e: SecurityException) {
                        _channel.send("Erro ao configurar Geofences: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Adiciona um novo local ao repositório.
     *
     * @param local O local a ser adicionado.
     */
    fun insertLocation(local: LocationDTO) {
        viewModelScope.launch {
            _loading.value = true

            locationRepo.insertLocation(local.copy(id = UUID.randomUUID().toString()))
                .onSuccess { _channel.send("Local salvo com sucesso!") }
                .onFailure { _channel.send("Erro ao salvar: ${it.message}") }

            _loading.value = false
        }
    }

    /**
     * Edita um local existente no repositório.
     *
     * @param local O local a ser atualizado.
     */
    fun updateLocation(local: LocationDTO) {
        viewModelScope.launch {
            _loading.value = true

            locationRepo.updateLocation(local)
                .onSuccess { _channel.send("Local atualizado com sucesso!") }
                .onFailure { e -> _channel.send("Salvo no dispositivo. Sincronização pendente: ${e.message}") }

            _loading.value = false
        }
    }

    /**
     * Remove um local do repositório.
     *
     * @param id O ID do local a ser removido.
     */
    fun deleteLocation(id: String) {
        viewModelScope.launch {
            _loading.value = true

            locationRepo.deleteLocation(id)
                .onSuccess { _channel.send("Local removido!") }
                .onFailure { _channel.send("Erro ao remover.") }

            _loading.value = false
        }
    }
}