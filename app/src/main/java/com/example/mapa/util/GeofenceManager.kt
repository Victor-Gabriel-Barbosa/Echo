package com.example.mapa.util

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.mapa.data.remote.dto.LocationDTO
import com.example.mapa.receiver.GeofenceBroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    private val prefs = context.getSharedPreferences("GeofencePrefs", Context.MODE_PRIVATE)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission") // Garanta que verificou a permissão antes de chamar isso
    fun setupGeofences(locations: List<LocationDTO>, currentUserUid: String?) {
        if (currentUserUid == null) return

        // Filtra locais de OUTROS usuários E que AINDA NÃO foram notificados
        val validLocations = locations.filter { location ->
            location.uid != currentUserUid && !prefs.getBoolean(location.id, false)
        }

        if (validLocations.isEmpty()) {
            geofencingClient.removeGeofences(geofencePendingIntent)
            return
        }

        // Cria a lista de Geofences do Google
        val geofenceList = validLocations.map { location ->
            Geofence.Builder()
                .setRequestId(location.id)
                .setCircularRegion(
                    location.latitude,
                    location.longitude,
                    location.radius.toFloat()
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()

        // Registra no sistema operacional
        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
            .addOnFailureListener {
                Log.e("GeofenceManager", "Erro ao configurar Geofences: ${it.message}")
            }
    }
}