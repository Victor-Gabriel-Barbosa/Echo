package com.example.mapa.ui.navigation

import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.example.mapa.R
import com.example.mapa.ui.component.AsyncImg
import com.example.mapa.ui.screen.ChatListScreen
import com.example.mapa.ui.screen.ChatScreen
import com.example.mapa.ui.screen.HomeScreen
import com.example.mapa.ui.screen.ProfileScreen
import com.example.mapa.ui.screen.SavedScreen
import com.example.mapa.viewmodels.AuthViewModel
import com.example.mapa.viewmodels.ChatListViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Composable principal que configura a estrutura de navegação do aplicativo.
 *
 * @param authViewModel O ViewModel para gerenciar a autenticação do usuário.
 * @param chatListViewModel O ViewModel para gerenciar a lista de conversas.
 */
@Composable
fun MapaNav(
    authViewModel: AuthViewModel = koinViewModel(),
    chatListViewModel: ChatListViewModel = koinViewModel(),
) {
    val context = LocalContext.current

    // Observáveis do ViewModel
    val userUiState by authViewModel.uiState.collectAsStateWithLifecycle()

    // Feedback visual (eventos) vindo do ViewModel
    LaunchedEffect(Unit) {
        authViewModel.channel.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Estado da navegação principal
    val backStack = rememberSaveable { mutableStateListOf<Routes>(Routes.Home) }
    val destination = backStack.lastOrNull()

    // Obtém dados enviados para a Activity através do Intent
    val activity = context as? ComponentActivity
    val intent = activity?.intent
    val contactUidIntent = intent?.getStringExtra("contactUid")
    val locationIdIntent = intent?.getStringExtra("locationId")

    LaunchedEffect(contactUidIntent, locationIdIntent) {
        if (contactUidIntent != null && locationIdIntent != null) {
            backStack.add(Routes.Chat(uid = contactUidIntent, localId = locationIdIntent))
            activity.intent = Intent()
        }
    }

    // Conta a quantidade de mensagens não lidas
    val unreadCount by chatListViewModel.unreadCount.collectAsStateWithLifecycle()

    NavigationSuiteScaffold(
        layoutType = if (destination is Routes.Chat) NavigationSuiteType.None else NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo()),
        navigationSuiteItems = {
            AppRoutes.entries.forEach { item ->
                // Verifica se o item está selecionado
                val selected = when (item.route) {
                    Routes.ChatRoutes -> destination == Routes.ChatRoutes || destination == Routes.ChatList || destination is Routes.Chat
                    else -> destination == item.route
                }

                item(
                    selected = selected,
                    onClick = {
                        if (destination != item.route) {
                            if (item.route == Routes.Home) {
                                backStack.clear()
                                backStack.add(Routes.Home)
                            } else {
                                if (backStack.last() != item.route) {
                                    backStack.removeAll { it == item.route }
                                    backStack.add(item.route)
                                }
                            }
                        }
                    },

                    icon = {
                        when (item) {
                            AppRoutes.PROFILE if userUiState.user?.photo != null -> {
                                AsyncImg(
                                    model = userUiState.user?.photo,
                                    contentDescription = stringResource(R.string.foto_de_perfil),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .border(
                                            width = if (selected) 2.dp else 0.dp,
                                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = CircleShape
                                        )
                                )
                            }

                            AppRoutes.CHAT if unreadCount > 0 -> {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                                            Text("$unreadCount")
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selected) item.iconFill else item.icon,
                                        contentDescription = stringResource(item.label),
                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            else -> {
                                Icon(
                                    imageVector = if (selected) item.iconFill else item.icon,
                                    contentDescription = stringResource(item.label),
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(item.label),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                modifier = Modifier.padding(innerPadding)
            ) { route ->
                when (route) {
                    Routes.Home -> NavEntry(route) {
                        HomeScreen(
                            user = userUiState.user,
                            onChat = { uid, locationId -> backStack.add(Routes.Chat(uid, locationId)) }
                        )
                    }

                    Routes.Saved -> NavEntry(route) {
                        SavedScreen()
                    }

                    Routes.ChatRoutes, Routes.ChatList -> NavEntry(route) {
                        ChatListScreen(
                            onChat = { uid, localId -> backStack.add(Routes.Chat(uid, localId)) },
                            chatListViewModel = chatListViewModel
                        )
                    }

                    is Routes.Chat -> NavEntry(route) {
                        ChatScreen(
                            uid = route.uid,
                            locationId = route.localId,
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }

                    Routes.Profile -> NavEntry(route) {
                        ProfileScreen(
                            authViewModel = authViewModel
                        )
                    }

                    else -> error("Rota não encontrada: $route")
                }
            }
        }
    }
}