package com.example.mapa.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.mapa.ui.theme.MapaTheme

/**
 * Composable que exibe uma imagem de forma assíncrona.
 *
 * @param model O modelo de dados da imagem.
 * @param contentDescription A descrição do conteúdo da imagem.
 * @param modifier O modificador para personalizar a exibição da imagem.
 * @param shape A forma da imagem.
 * @param contentScale A escala de conteúdo da imagem.
 * @param errorIcon O ícone de erro a ser exibido em caso de falha na carga da imagem.
 * @param loadingContent O conteúdo a ser exibido enquanto a imagem está sendo carregada.
 * @param errorContent O conteúdo a ser exibido em caso de falha na carga da imagem.
 */
@Composable
fun AsyncImg(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    contentScale: ContentScale = ContentScale.Crop,
    errorIcon: ImageVector = Icons.Default.BrokenImage,
    loadingContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LoadingAnimation()
        }
    },
    errorContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = errorIcon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest
                .Builder(LocalContext.current)
                .data(model)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            loading = { loadingContent() },
            error = { errorContent() }
        )
    }
}

@Preview
@Composable
private fun AsyncImgPreview() {
    MapaTheme {
        AsyncImg(
            model = "https://placekitten.com/200/200",
            modifier = Modifier.size(48.dp),
            contentDescription = null
        )
    }
}