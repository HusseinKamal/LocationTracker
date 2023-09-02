package com.hussein.locationtracker

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.ConditionVariable
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LocationService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient : LocationClient

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action){
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = DefaultLocationClient(
            this,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
    }
    private fun start(){
        val notification = NotificationCompat.Builder(this,"location")
            .setContentTitle("Tracking Location....")
            .setContentText("Location: null")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setOngoing(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        locationClient
            .getLocationUpdates(10000L)
            .catch { ex->
                ex.printStackTrace()
            }
            .onEach {
                val lat = it.latitude.toString().takeLast(3)
                val log = it.longitude.toString().takeLast(3)
                val  updateLocation = notification.setContentText(
                    "Location : ($lat , $log)"
                )
                notificationManager.notify(1,updateLocation.build())
            }
            .launchIn(serviceScope)
        startForeground(1,notification.build())
    }
    private fun stop(){
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"

    }
}