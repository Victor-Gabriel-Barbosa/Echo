package com.example.mapa.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mapa.R
import com.example.mapa.data.remote.dto.UserDTO
import com.example.mapa.model.UserUiState
import com.example.mapa.ui.components.LoadingAnimation
import com.example.mapa.ui.components.AsyncImg
import com.example.mapa.ui.components.ConfirmDialog
import com.example.mapa.ui.components.EditDialog
import com.example.mapa.ui.components.Header
import com.example.mapa.ui.theme.MapaTheme
import com.example.mapa.viewmodels.AuthViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = koinViewModel()
) {
    val context = LocalContext.current

    // Observáveis do ViewModel
    val usuarioUiState by authViewModel.uiState.collectAsStateWithLifecycle()

    // Feedback visual (eventos) vindo do ViewModel
    LaunchedEffect(Unit) {
        authViewModel.channel.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Abre as configurações do aplicativo
    val openAppSettings = {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        context.startActivity(intent)
    }

    ProfileScreenContent(
        onLogout = authViewModel::logout,
        onEditPhoto = authViewModel::updatePhoto,
        onEditName = authViewModel::updateName,
        onOpenSettings = openAppSettings,
        userUiState = usuarioUiState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    onLogout: () -> Unit,
    onEditPhoto: (String) -> Unit,
    onEditName: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    userUiState: UserUiState
) {
    // Estado dos diálogo de edição
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }

    // Launcher de seleção de imagem
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) onEditPhoto(uri.toString())
        }
    )

    // Dialog de edição de nome
    EditDialog(
        visible = showEditDialog,
        initialText = userUiState.user?.name ?: "",
        title = stringResource(R.string.editar_nome),
        label = stringResource(R.string.nome),
        onDismiss = { showEditDialog = false },
        onConfirm = {
            onEditName(it)
            showEditDialog = false
        }
    )

    // Dialog de confirmação de logout
    ConfirmDialog(
        visible = showConfirmDialog,
        title = stringResource(R.string.sair_pergunta),
        text = stringResource(R.string.tem_certeza_que_deseja_sair),
        onDismiss = { showConfirmDialog = false },
        textConfirm = stringResource(R.string.sair),
        onConfirm = onLogout
    )

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Header(
                title = stringResource(R.string.perfil),
                icon = R.drawable.logo,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
                    tooltip = {
                        PlainTooltip {
                            Text(stringResource(R.string.configuracoes_do_app))
                        }
                    },
                    state = rememberTooltipState()
                ) {
                    IconButton(
                        onClick = onOpenSettings
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.configuracoes_do_app)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(132.dp),
                contentAlignment = Alignment.Center
            ) {
                // Avatar com feedback de carregamento
                if (userUiState.loadingPhoto) LoadingAnimation(size = 48.dp)
                else AsyncImg(
                    model = userUiState.user?.photo,
                    contentDescription = stringResource(R.string.foto_de_perfil),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(132.dp)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )

                Box(
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    // Botão de alterar foto com dica de uso
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = {
                            PlainTooltip {
                                Text(stringResource(R.string.alterar_foto))
                            }
                        },
                        state = rememberTooltipState()
                    ) {
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            shape = CircleShape,
                            enabled = !userUiState.loadingPhoto,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CameraAlt,
                                contentDescription = stringResource(R.string.alterar_foto),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Badge de avaliação
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.height(0.dp))

                    Text(
                        text = "${userUiState.user?.averageRating ?: 0.0}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = userUiState.user?.name ?: stringResource(R.string.usu_rio_desconhecido),
                    style = MaterialTheme.typography.headlineMedium
                )

                // Botão de editar nome com dica de uso
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    tooltip = {
                        PlainTooltip {
                            Text(stringResource(R.string.editar_nome))
                        }
                    },
                    state = rememberTooltipState()
                ) {
                    IconButton(
                        onClick = { showEditDialog = !showEditDialog },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.editar_nome),
                        )
                    }
                }
            }

            Text(
                text = userUiState.user?.email ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showConfirmDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(text = stringResource(R.string.sair_da_conta))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenContentPreview() {
    MapaTheme {
        ProfileScreenContent(
            userUiState = UserUiState(
                UserDTO (
                    uid = "123",
                    name = "João da Silva",
                    email = "joaosilva@example.com",
                    photo = "https://cdn-icons-png.flaticon.com/512/12225/12225881.png"
                ),
                loggedIn = true,
                loadingPhoto = false,
                loadingName = false
            ),
            onLogout = {},
            onEditPhoto = {},
            onEditName = {},
            onOpenSettings = {}
        )
    }
}