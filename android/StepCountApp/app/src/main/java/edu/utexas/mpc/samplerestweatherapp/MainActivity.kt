package edu.utexas.mpc.samplerestweatherapp

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage

class MainActivity : AppCompatActivity() {


    // I'm using lateinit for these widgets because I read that repeated calls to findViewById
    // are energy intensive
    lateinit var tempView: TextView
    lateinit var retrieveButton: Button
    lateinit var iconView: ImageView
    lateinit var syncButton: Button
    lateinit var stepView: TextView
    lateinit var publishButton: Button
    lateinit var networkButton: Button
    lateinit var instrView: TextView
    lateinit var caloriesButton: Button

    lateinit var queue: RequestQueue
    lateinit var gson: Gson
    lateinit var mostRecentWeatherResult: WeatherResult

    var forecast: String = ""

    // MQTT stuff
    lateinit var mqttAndroidClient: MqttAndroidClient
    val serverUri = "tcp://192.168.4.1:1883"
    val clientId = "EmergingTechMQTTClient"
    val subscribeTopic = "steps"
    val publishTopic = "weather"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tempView = this.findViewById(R.id.temp)
        retrieveButton = this.findViewById(R.id.retrieveWeather)
        iconView = this.findViewById(R.id.weatherIconView)
        syncButton = this.findViewById(R.id.syncButtonSteps)
        stepView = this.findViewById(R.id.steps)
        networkButton = this.findViewById(R.id.networkButtonSteps)
        publishButton = this.findViewById(R.id.publishStep)
        instrView = this.findViewById(R.id.InstructionViewSteps)
        caloriesButton = this.findViewById(R.id.calorieSwitchButton)

        // when the user presses the syncbutton, this method will get called
        retrieveButton.setOnClickListener({ requestWeather() })
        syncButton.setOnClickListener({ syncWithPi() })
        publishButton.setOnClickListener({ publish() })
        networkButton.setOnClickListener({ changeNetwork() })
        caloriesButton.setOnClickListener({ changeActivity() })


        queue = Volley.newRequestQueue(this)
        gson = Gson()

        mqttAndroidClient = MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        instructions (0);

        mqttAndroidClient.setCallback(object : MqttCallbackExtended {
            // when the client is successfully connected to the broker, this method gets called
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                println("Connection Complete!!")
                // this subscribes the client to the subscribe topic
                mqttAndroidClient.subscribe(subscribeTopic, 0)
            }

            // this method is called when a message is received that fulfills a subscription
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val count = message.toString()
                stepView.setText(count)
            }

            override fun connectionLost(cause: Throwable?) {
                println("Connection Lost")
            }

            // this method is called when the client successfully publishes to the broker
            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                println("Delivery Complete")
            }
        })
    }

    fun requestWeather() {
        val url = StringBuilder("https://api.openweathermap.org/data/2.5/onecall?lat=30.27&lon=-97.7431&exclude=current,minutely,hourly&units=imperial&appid={API Key}").toString()
        val stringRequest = object : StringRequest(com.android.volley.Request.Method.GET, url,
                com.android.volley.Response.Listener<String> { response ->
                    mostRecentWeatherResult = gson.fromJson(response, WeatherResult::class.java)
                    val today: Daily = mostRecentWeatherResult.daily[0]
                    val tomorrow: Daily = mostRecentWeatherResult.daily[1]
                    val iconId = today.weather[0].icon
                    val iconURL = "https://openweathermap.org/img/wn/$iconId@4x.png"
                    println(iconURL)
                    Picasso.with(this).load(iconURL).resize(100, 100).into(iconView)
                    tempView.text = today.temp.day.toString()
                    val temp_max_today = today.temp.max
                    val temp_min_today = today.temp.min
                    val hum_today = today.humidity
                    val temp_max_tom = tomorrow.temp.max
                    val temp_min_tom = tomorrow.temp.min
                    val hum_tom = tomorrow.humidity
                    forecast = "{'today': ($temp_max_today, $temp_min_today, $hum_today), 'tomorrow': ($temp_min_tom, $temp_max_tom, $hum_tom)}"
                    instructions (1)
                },
                com.android.volley.Response.ErrorListener { println("******That didn't work!") }) {}
        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }

    fun changeNetwork() {
        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        instructions (2)
    }

    fun syncWithPi() {
        println("+++++++ Connecting...")
        mqttAndroidClient.connect()
        instructions (3)
    }

    fun publish() {
        val message = MqttMessage()
        message.payload = forecast.toByteArray()
        // this publishes a message to the publish topic
        if (mqttAndroidClient.isConnected) {
            mqttAndroidClient.publish(publishTopic, message)
            instructions (4)
        }
    }

    fun changeActivity() {
        val intent = Intent(this, MainActivity2::class.java)
        startActivity(intent)
    }

    fun instructions (instrStep: Int) {
        if (instrStep == 0) {
            instrView.text = "Get the weather in Austin!"
        } else if (instrStep == 1) {
            instrView.text = "Change wifi network"
        } else if (instrStep == 2){
            instrView.text = "Sync with Pi"
        } else if (instrStep == 3) {
            instrView.text = "Check if you have reached your step goal!"
        } else {
            instrView.text = ""
        }
    }
}

class WeatherResult(val daily: Array<Daily>)
class Coordinates(val lon: Double, val lat: Double)
class Temp(val day: Double, val min: Double, val max: Double)
class Daily(val temp: Temp, val humidity: Double, val weather: Array<Weather>)
class Weather(val icon: String)
