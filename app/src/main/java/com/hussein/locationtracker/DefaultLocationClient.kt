package com.hussein.locationtracker

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class DefaultLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient):LocationClient {

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(interval: Long): Flow<Location> {
        return callbackFlow {
            if(!context.hasLocationPermission()){
                throw LocationClient.LocationException("Missing Location permission")
            }
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val hasGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if(!hasGPS && !isNetworkEnabled){
                throw LocationClient.LocationException("GPS is disabled")
            }

            val request = LocationRequest.create()
                .setInterval(interval)
                .setFastestInterval(interval)
            val locationCall = object :LocationCallback(){
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)
                    result.locations.lastOrNull()?.let {location ->
                        launch { send(location) }
                    }
                }
            }
            client.requestLocationUpdates(request,locationCall, Looper.getMainLooper())

            awaitClose{client.removeLocationUpdates(locationCall)}
        }
    }
}