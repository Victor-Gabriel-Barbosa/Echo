package com.example.mapa.util

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.example.mapa.R

/**
 * Solicita permissões para o usuário.
 *
 * @param onPermsChange Callback que é chamado quando as permissões são alteradas.
 */
@Composable
fun ReqPerms(
    onPermsChange: (Boolean, Boolean) -> Unit
) {
    val context = LocalContext.current

    // Controle de exibição do diálogo de explicação
    var showBgRationale by remember { mutableStateOf(false) }

    // Permissões de localização em primeiro plano
    val fgPerms = remember {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    // Permissão de notificação
    val notificationPerm = Manifest.permission.POST_NOTIFICATIONS

    // Launcher para permissão de segundo plano
    val bgLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { ok ->
        onPermsChange(true, ok)
    }

    // Launcher para permissão de notificação
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    // Solicita permissões de primeiro plano
    val fgLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { ok ->
        val fgOk =
            ok[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    ok[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fgOk) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val notifOk = ContextCompat.checkSelfPermission(
                    context,
                    notificationPerm
                ) == PackageManager.PERMISSION_GRANTED

                if (!notifOk) notificationLauncher.launch(notificationPerm)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val bgOk = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (!bgOk) {
                    onPermsChange(true, false)
                    showBgRationale = true
                } else onPermsChange(true, true)
            } else onPermsChange(true, true)
        } else onPermsChange(false, false)
    }

    // Verifica se as permissões já foram concedidas
    LaunchedEffect(Unit) {
        val fgOk = fgPerms.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (fgOk) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val notifOk = ContextCompat.checkSelfPermission(
                    context,
                    notificationPerm
                ) == PackageManager.PERMISSION_GRANTED

                if (!notifOk) notificationLauncher.launch(notificationPerm)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val bgOk = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (!bgOk) {
                    onPermsChange(true, false)
                    showBgRationale = true
                } else onPermsChange(true, true)
            } else onPermsChange(true, true)
        } else fgLauncher.launch(fgPerms)
    }

    // Exibe diálogo de explicação para permissão de segundo plano
    if (showBgRationale) {
        AlertDialog(
            onDismissRequest = { showBgRationale = false },
            title = {
                Text(stringResource(R.string.aviso_de_permissao))
            },
            text = {
                Text(
                    text = stringResource(R.string.explicacao_permissao_background)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBgRationale = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            bgLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                    }
                ) {
                    Text(stringResource(R.string.continuar))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBgRationale = false }
                ) {
                    Text(stringResource(R.string.agora_nao))
                }
            }
        )
    }
}