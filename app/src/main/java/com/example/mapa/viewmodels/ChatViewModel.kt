package com.example.mapa.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mapa.data.remote.dto.ChatDTO
import com.example.mapa.data.remote.dto.MsgDTO
import com.example.mapa.data.remote.dto.UserDTO
import com.example.mapa.data.remote.datasource.AuthRemote
import com.example.mapa.data.repository.ChatRepository
import com.example.mapa.data.repository.UserRepository
import com.example.mapa.model.ChatUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel para a tela de chat.
 *
 * @property authRemote Repositório para operações de autenticação.
 * @property chatRepo Repositório para operações relacionadas a chats.
 * @property userRepo Repositório para operações relacionadas a usuários.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    authRemote: AuthRemote,
    private val chatRepo: ChatRepository,
    private val userRepo: UserRepository,
) : ViewModel() {
    /**
     * O UID do destinatário (usuário selecionado na lista de chats).
     */
    private val _contactUid = MutableStateFlow<String?>(null)

    /**
     * O ID do local selecionado na lista de chats.
     */
    private val _locationId = MutableStateFlow<String?>(null)

    /**
     * O estado de carregamento da tela.
     */
    private val _loading = MutableStateFlow(false)

    /**
     * Canal para enviar mensagens de eventos para a UI.
     */
    private val _channel = Channel<String>(Channel.BUFFERED)
    val channel = _channel.receiveAsFlow()

    /**
     * O UID do autor (usuário logado).
     */
    val authorUid: StateFlow<String?> = authRemote.user
        .map { it?.uid }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Combinação de dados entre o autor e o contato para carregar as mensagens.
     */
    private val chatFlow = combine(
        authorUid.filterNotNull(),
        _contactUid.filterNotNull()
    ) { myUid, contactUid -> Pair(myUid, contactUid) }
        .flatMapLatest { (myUid, contactUid) ->
            combine(
                loadMsgsFlow(generateId(myUid, contactUid)),
                loadContactFlow(contactUid),
            ) { msgs, contact -> Triple(msgs, contact, myUid) }
        }

    /**
     * O estado da UI para a tela de chat.
     */
    val uiState: StateFlow<ChatUiState> = combine(
        chatFlow,
        _loading
    ) { (msgs, contact, authorUid), loading ->
        ChatUiState(
            msgs = msgs,
            contact = contact,
            uid = authorUid,
            loading = loading,
            loadingPhoto = contact?.photo == null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState(loading = true, loadingPhoto = true)
    )

    /**
     * Inicializa o ViewModel com o UID do autor e do contato.
     *
     * @param uid O UID do autor.
     * @param locationId O ID do local selecionado.
     */
    fun initialize(uid: String, locationId: String) {
        if (_contactUid.value != uid) _contactUid.value = uid
        if (_locationId.value != locationId) _locationId.value = locationId
    }

    /**
     * Carrega as mensagens do chat.
     *
     * @param id O ID da sala de chat.
     * @return Um fluxo de mensagens do chat.
     */
    private fun loadMsgsFlow(id: String): Flow<List<MsgDTO>> {
        return chatRepo.getMsgs(id)
            .catch { e ->
                Log.e("ChatViewModel", "loadMsgsFlow: ${e.message}")
                _channel.send("Erro ao carregar mensagens: ${e.message}")
                emit(emptyList())
            }
    }

    /**
     * Carrega as informações do contato.
     *
     * @param uid O UID do contato.
     * @return Um [Flow] de informações do contato.
     */
    private fun loadContactFlow(uid: String): Flow<UserDTO?> {
        return userRepo.getUser(uid)
            .catch {
                Log.e("ChatViewModel", "loadContactFlow: ${it.message}")
                emit(null)
            }
    }

    /**
     * Envia uma nova mensagem.
     *
     * @param msg A mensagem a ser enviada.
     */
    fun sendMsg(msg: MsgDTO) {
        if (msg.text.isBlank() && msg.imgUrls.isEmpty()) return

        val authorUid = this@ChatViewModel.authorUid.value ?: return
        val contactUid = _contactUid.value ?: return
        val id = generateId(authorUid, contactUid)
        val locationId = _locationId.value ?: return

        val newMsg = msg.copy(
            id = UUID.randomUUID().toString(),
            text = msg.text.trim(),
            uid = authorUid,
            timestamp = System.currentTimeMillis(),
            read = false
        )

        val chatSummary = ChatDTO(
            id = id,
            lastMsg = newMsg,
            lastTimestamp = newMsg.timestamp,
            participants = listOf(authorUid, contactUid),
            visibleTo = listOf(authorUid, contactUid),
            locationId = locationId
        )

        viewModelScope.launch {
            _loading.value = true
            chatRepo.insertMsg(newMsg, chatSummary)
                .onFailure { e ->
                    Log.e("ChatViewModel", "sendMsg: ${e.message}")
                    _channel.send("Falha ao enviar: ${e.message}")
                }
            _loading.value = false
        }
    }

    /**
     * Edita uma mensagem existente.
     *
     * @param newMsg A nova mensagem.
     */
    fun updateMsg(newMsg: MsgDTO) {
        val myUid = authorUid.value ?: return
        val contactUid = _contactUid.value ?: return
        val id = generateId(myUid, contactUid)

        viewModelScope.launch {
            _loading.value = true
            chatRepo.updateMsg(id, newMsg.copy(edited = true, text = newMsg.text.trim()))
                .onFailure { e ->
                    Log.e("ChatViewModel", "updateMsg: ${e.message}")
                    _channel.send("Falha ao atualizar: ${e.message}")
                }
            _loading.value = false
        }
    }

    /**
     * Exclui uma mensagem.
     *
     * @param msgId O ID da mensagem a ser excluída.
     */
    fun deleteMsg(msgId: String) {
        val myUid = authorUid.value ?: return
        val contactUid = _contactUid.value ?: return
        val id = generateId(myUid, contactUid)

        viewModelScope.launch {
            _loading.value = true
            chatRepo.deleteMsg(id, msgId)
                .onFailure { e ->
                    Log.e("ChatViewModel", "deleteMsg: ${e.message}")
                    _channel.send("Falha ao excluir: ${e.message}")
                }
            _loading.value = false
        }
    }

    /**
     * Marca as mensagens como lidas.
     *
     * @param id O ID da sala de chat.
     * @param msgs As mensagens a serem marcadas como lidas.
     */
    fun markMsgsAsRead(visibleMsgIds: List<String>, read: Boolean = true) {
        val contactUid = _contactUid.value ?: return
        val myUid = authorUid.value ?: return
        val chatId = generateId(myUid, contactUid)

        // Filtra para pegar apenas mensagens do contato que ainda não foram lidas e estão visíveis
        val msgsToUpdate = uiState.value.msgs.filter { msg ->
            msg.uid == contactUid && !msg.read && msg.id in visibleMsgIds
        }.map { it.id }

        if (msgsToUpdate.isNotEmpty()) {
            viewModelScope.launch {
                chatRepo.updateMsgsRead(chatId, msgsToUpdate, read)
                    .onFailure { e ->
                        Log.e("ChatViewModel", "markVisibleMsgsAsRead: ${e.message}")
                        _channel.send("Falha ao marcar como lidas: ${e.message}")
                    }
            }
        }
    }

    /**
     * Avalia o usuário.
     *
     * @param rating A nota do usuário.
     */
    fun rateUser(rating: Double) {
        val myUid = authorUid.value ?: return
        val contact = uiState.value.contact ?: return

        viewModelScope.launch {
            userRepo.updateUserRating(contact, myUid, rating)
                .onFailure { e ->
                    Log.e("ChatViewModel", "rateUser: ${e.message}")
                    _channel.send("Falha ao avaliar: ${e.message}")
                }
        }
    }

    /**
     * Gera um ID de sala com base nos UIDs dos participantes.
     *
     * @param user1 O UID do primeiro participante.
     * @param user2 O UID do segundo participante.
     * @return O ID da sala gerado.
     */
    private fun generateId(user1: String, user2: String): String {
        return if (user1 < user2) "${user1}_${user2}" else "${user2}_${user1}"
    }
}