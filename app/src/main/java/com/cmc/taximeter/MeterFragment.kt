package com.cmc.taximeter

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.*
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import android.widget.Toast
import androidx.core.app.NotificationCompat
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// --- New Retrofit Interface and Data Classes (PLACEHOLDERS: Implement these in separate files) ---

// 1. Weather API Interface (Replace with your actual structure if using a different API)
interface WeatherApi {
    @GET("data/2.5/weather")
    fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric", // Celsius
        @Query("appid") apiKey: String
    ): Call<WeatherResponse>
}

// 2. Weather Data Model (Simplified for temperature only)
data class WeatherResponse(
    val main: Main
)
data class Main(
    val temp: Float
)
// ------------------------------------------------------------------------------------------------

class MeterFragment : Fragment(R.layout.fragment_meter) {

    private lateinit var tvDistance: TextView // Maps to R.id.textViewDistance
    private lateinit var tvTempsEcoule: TextView // Maps to R.id.textViewTime
    private lateinit var tvTotalAPayer: TextView // Maps to R.id.textViewTotalFare

    // New TextViews from the digital design
    private lateinit var tvTemperature: TextView // Maps to R.id.textViewTemperature
    private lateinit var tvHdRate: TextView

    private lateinit var btnDemarrerTaxi: Button // Maps to R.id.btnDemarrer
    private lateinit var btnReset: Button // Maps to R.id.btnRestart
    private lateinit var btnStop: Button

    private lateinit var clientLocalisation: FusedLocationProviderClient

    private var heureDebut: Long = 0
    private var localisationActuelle: Location? = null
    private var distanceTotale: Float = 0f
    private var courseEnCours: Boolean = false
    private var tarifDeBase: Float = 2.5f
    private var prixParKm: Float = 2.0f

    // Constants for weather and permissions
    private val WEATHER_API_KEY = "4cd29e7ea36ffeaef7cad09a75f90f6c" // <--- REPLACE THIS
    private val BASE_URL = "https://api.openweathermap.org/"

    private val PERMISSION_LOCALISATION = 123
    private val CHANNEL_ID = "course_channel"
    private val NOTIFICATION_ID = 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialisation des vues - UPDATED TO NEW IDS
        tvDistance = view.findViewById(R.id.textViewDistance)
        tvTempsEcoule = view.findViewById(R.id.textViewTime)
        tvTotalAPayer = view.findViewById(R.id.textViewTotalFare)

        // New TextViews initialization
        tvTemperature = view.findViewById(R.id.textViewTemperature)

        // Button initialization - UPDATED TO NEW IDS
        btnDemarrerTaxi = view.findViewById(R.id.btnDemarrer)
        btnReset = view.findViewById(R.id.btnRestart)
        btnStop = view.findViewById(R.id.btnStop)

        // Initialisation du client de localisation
        clientLocalisation = LocationServices.getFusedLocationProviderClient(requireContext())

        // Demarrer la course
        btnDemarrerTaxi.setOnClickListener {
            if (EasyPermissions.hasPermissions(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                demarrerCourse()
            } else {
                EasyPermissions.requestPermissions(
                    PermissionRequest.Builder(this, PERMISSION_LOCALISATION, Manifest.permission.ACCESS_FINE_LOCATION)
                        .build()
                )
            }
        }

        // Réinitialiser la course
        btnReset.setOnClickListener {
            resetCourse()
        }

        // Arrêter la course
        btnStop.setOnClickListener {
            stopCourse()
        }
    }

    private fun demarrerCourse() {
        courseEnCours = true
        heureDebut = SystemClock.elapsedRealtime()

        btnDemarrerTaxi.isEnabled = false
        btnDemarrerTaxi.isClickable = false

        // Lancer la coroutine pour mettre à jour la distance, le temps, et la température
        CoroutineScope(Dispatchers.Main).launch {
            while (courseEnCours) {
                val localisation = obtenirLocalisation()
                localisation?.let {
                    // Calcul du temps écoulé
                    val tempsEcouleSec = (SystemClock.elapsedRealtime() - heureDebut) / 1000
                    val minutes = tempsEcouleSec / 60
                    val secondes = tempsEcouleSec % 60
                    val tempsFormatte = String.format("%02d:%02d", minutes, secondes)

                    // Mise à jour de la distance et du montant total
                    tvDistance.text = "%.2f".format(distanceTotale / 1000)
                    tvTempsEcoule.text = tempsFormatte
                    tvTotalAPayer.text = "%.2f".format(calculerMontantTotal(distanceTotale))

                    // Mettre à jour la température
                    fetchWeather(localisation.latitude, localisation.longitude)

                    delay(1000)  // Met à jour toutes les secondes
                }
            }
        }
    }

    private fun fetchWeather(lat: Double, lon: Double) {
        if (WEATHER_API_KEY == "YOUR_OPENWEATHER_API_KEY") {
            tvTemperature.text = "API Key Missing"
            return
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(WeatherApi::class.java)
        val call = service.getWeather(lat, lon, "metric", WEATHER_API_KEY) // "metric" for Celsius

        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val tempCelsius = response.body()?.main?.temp
                    if (tempCelsius != null) {
                        // Format the temperature to show as "22°"
                        tvTemperature.text = "${tempCelsius.toInt()}°"
                    } else {
                        tvTemperature.text = "--°"
                    }
                } else {
                    // Handle API error response
                    tvTemperature.text = "Error ${response.code()}"
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                // Handle network failure
                tvTemperature.text = "No Weather"
            }
        })
    }

    private fun stopCourse() {
        btnDemarrerTaxi.isEnabled = true  // Réactive le bouton
        btnDemarrerTaxi.isClickable = true

        courseEnCours = false
        btnDemarrerTaxi.text = "DÉMARRER"  // Remet le texte du bouton

        // Afficher un message ou un toast indiquant que la course est terminée
        Toast.makeText(requireContext(), "Course terminée", Toast.LENGTH_SHORT).show()

        // Appeler la méthode pour afficher la notification de fin de course
        afficherNotificationFinCourse()
    }

    private fun resetCourse() {
        btnDemarrerTaxi.isEnabled = true  // Réactive le bouton
        btnDemarrerTaxi.isClickable = true

        courseEnCours = false
        distanceTotale = 0f
        tvDistance.text = "0.00"
        tvTempsEcoule.text = "00:00"
        tvTotalAPayer.text = "0.00"
        tvTemperature.text = "--°" // Reset temperature display
        tvHdRate.text = "0.00" // Reset H/D display
        btnDemarrerTaxi.text = "DÉMARRER"
        // Réinitialiser tout et afficher un message
        Toast.makeText(requireContext(), "Course réinitialisée", Toast.LENGTH_SHORT).show()
    }

    private suspend fun obtenirLocalisation(): Location? {
        return withContext(Dispatchers.IO) {
            try {
                val resultatLocalisation: Task<Location> = clientLocalisation.lastLocation
                val localisation = Tasks.await(resultatLocalisation)
                if (localisation != null) {
                    if (localisationActuelle != null) {
                        distanceTotale += localisation.distanceTo(localisationActuelle!!)
                    }
                    localisationActuelle = localisation
                }
                localisationActuelle
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun calculerMontantTotal(distance: Float): Float {
        val distanceEnKm = distance / 1000
        return tarifDeBase + (distanceEnKm * prixParKm)
    }

    private fun afficherNotificationFinCourse() {
        // Créer un canal de notification (nécessaire pour Android O et plus)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notifications de course",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Canal pour les notifications de fin de course"
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Création de la notification
        val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.map)  // Remplacez par votre icône de notification
            .setContentTitle("Course terminée")
            .setContentText("Distance : %.2f km\nTarif total : %.2f Dhs".format(distanceTotale / 1000, calculerMontantTotal(distanceTotale)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Afficher la notification
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (EasyPermissions.hasPermissions(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)) {
            demarrerCourse()
        } else {
            Toast.makeText(requireContext(), "Permission refusée", Toast.LENGTH_SHORT).show()
        }
    }
}