package com.example.mapa.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.example.mapa.MainActivity
import com.example.mapa.R
import com.example.mapa.data.repository.LocationRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.HttpURLConnection
import java.net.URL

/**
 * BroadcastReceiver para lidar com eventos de geofence.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver(), KoinComponent {
    // Injeção de dependência para o repositório de locais
    private val locationRepository: LocationRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        // Verifica se o evento de geofence é válido
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            val errorCode = geofencingEvent?.errorCode ?: -1
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(errorCode)
            Log.e(TAG, "Erro no Geofence: $errorMessage")
            return
        }

        // Processa a transição de geofence
        when (geofencingEvent.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> handleEnterTransition(context, geofencingEvent)
            Geofence.GEOFENCE_TRANSITION_EXIT -> handleExitTransition(context, geofencingEvent)
            else -> Log.w(TAG, "Transição ignorada: ${geofencingEvent.geofenceTransition}")
        }
    }

    private fun handleEnterTransition(context: Context, event: GeofencingEvent) {
        val triggeringGeofences = event.triggeringGeofences ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                createNotificationChannel(context)

                for (geofence in triggeringGeofences) {
                    val locationId = geofence.requestId

                    // Verifica se o local já foi notificado
                    if (prefs.getBoolean(locationId, false)) { continue }

                    // Busca o local pelo ID
                    val dto = locationRepository.getLocationById(locationId)
                    if (dto == null) {
                        Log.w(TAG, "Local não encontrado para locationId=$locationId")
                        continue
                    }

                    // Baixa a primeira imagem
                    val imageUrl = dto.imgUrls.firstOrNull()
                    val bitmap = downloadImage(imageUrl)

                    // Mostra a notificação
                    showNotification(
                        context = context,
                        locationId = locationId,
                        title = dto.name,
                        description = dto.description,
                        image = bitmap
                    )

                    // Marca como notificado
                    prefs.edit { putBoolean(locationId, true) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar entrada no geofence", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    // Lida com a ação de sair do geofence
    private fun handleExitTransition(context: Context, event: GeofencingEvent) {
        val triggeringGeofences = event.triggeringGeofences ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        prefs.edit {
            for (geofence in triggeringGeofences) {
                remove(geofence.requestId)
            }
        }
    }

    // Baixa uma imagem a partir de uma URL
    private fun downloadImage(urlString: String?): Bitmap? {
        if (urlString.isNullOrBlank()) return null

        return try {
            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                doInput = true
            }

            connection.inputStream.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao baixar imagem", e)
            null
        }
    }

    // Mostra uma notificação
    private fun showNotification(
        context: Context,
        locationId: String,
        title: String,
        description: String,
        image: Bitmap?
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Cria um PendingIntent para abrir a MainActivity ao clicar na notificação
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("locationId", locationId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            locationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cria a notificação
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText(description)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Define a imagem na notificação
        if (image != null) {
            builder.setLargeIcon(image)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(image)
                    .bigLargeIcon(null as Bitmap?)
            )
        } else {
            builder.setStyle(
                NotificationCompat.BigTextStyle().bigText(description)
            )
        }

        // Mostra a notificação
        notificationManager.notify(locationId.hashCode(), builder.build())
    }

    // Cria um canal de notificação
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Verifica se o canal já existe
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificações de proximidade de locais no mapa"
        }

        // Cria o canal
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
        private const val CHANNEL_ID = "geofence_channel"
        private const val CHANNEL_NAME = "Alertas de Mapa"
        private const val PREFS_NAME = "GeofencePrefs"
    }
}