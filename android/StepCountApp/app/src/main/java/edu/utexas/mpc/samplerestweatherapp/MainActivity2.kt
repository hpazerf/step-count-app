package edu.utexas.mpc.samplerestweatherapp

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main2.*
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage

class MainActivity2 : AppCompatActivity() {

    lateinit var stepSwitchButton: Button
    lateinit var changeNetworkButton: Button
    lateinit var syncWithPiButton: Button
    lateinit var heightInput: EditText
    lateinit var weightInput: EditText
    lateinit var ageInput: EditText
    lateinit var goalInput: EditText
    lateinit var checkGoalButton: Button
    lateinit var gifView: ImageView
    lateinit var goalMsg: TextView

    lateinit var queue: RequestQueue
    lateinit var gson: Gson

    // MQTT stuff
    lateinit var mqttAndroidClient: MqttAndroidClient
    val serverUri = "tcp://192.168.4.1:1883"
    val clientId = "EmergingTechMQTTClient"
    val subscribeTopic = "calories"
    val publishTopic = "personal"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        stepSwitchButton = this.findViewById(R.id.stepsSwitchButton)
        changeNetworkButton = this.findViewById(R.id.networkButtonCalories)
        syncWithPiButton = this.findViewById(R.id.syncButtonCalories)
        heightInput = this.findViewById(R.id.heightInput)
        weightInput = this.findViewById(R.id.weightInput)
        ageInput = this.findViewById(R.id.ageInput)
        goalInput = this.findViewById(R.id.calorieGoalInput)
        checkGoalButton = this.findViewById(R.id.publishCalories)
        gifView = this.findViewById(R.id.gifView)
        goalMsg = this.findViewById(R.id.goalMsg)

        stepSwitchButton.setOnClickListener({ changeActivity() })
        changeNetworkButton.setOnClickListener({ changeNetwork() })
        syncWithPiButton.setOnClickListener({ syncWithPi() })
        checkGoalButton.setOnClickListener({ publish() })

        queue = Volley.newRequestQueue(this)
        gson = Gson()

        mqttAndroidClient = MqttAndroidClient(getApplicationContext(), serverUri, clientId);

        mqttAndroidClient.setCallback(object : MqttCallbackExtended {
            // when the client is successfully connected to the broker, this method gets called
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                println("Connection Complete!!")
                // this subscribes the client to the subscribe topic
                mqttAndroidClient.subscribe(subscribeTopic, 0)
            }

            // this method is called when a message is received that fulfills a subscription
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val metGoal = message.toString().toLowerCase().toBoolean()
                goalMsg.text = if(metGoal) "You did it!" else "Keep trying"
                requestGif(metGoal)
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

    fun requestGif(metGoal: Boolean) {
        val query: String = if(metGoal) "success" else "failed"
        val url = StringBuilder("https://api.giphy.com/v1/gifs/random?api_key={API KEY}&tag=$query").toString()
        val stringRequest = object : StringRequest(com.android.volley.Request.Method.GET, url,
                com.android.volley.Response.Listener<String> { response ->
                    val result: GiphyResult = gson.fromJson(response, GiphyResult::class.java)
                    println(result.data.image_original_url)
                    Glide.with(this).load(result.data.image_original_url).into(gifView)
                },
                com.android.volley.Response.ErrorListener { println("******That didn't work!") }) {}
        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }

    fun changeNetwork() {
        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
    }

    fun syncWithPi() {
        println("+++++++ Connecting...")
        mqttAndroidClient.connect()
    }

    fun publish() {
        val message = MqttMessage()
        val height = heightInput.text
        val weight = weightInput.text
        val age = ageInput.text
        val goal = calorieGoalInput.text
        val out = "($height, $weight, $age, $goal)"
        message.payload =  out.toByteArray()
        // this publishes a message to the publish topic
        if (mqttAndroidClient.isConnected) {
            mqttAndroidClient.publish(publishTopic, message)
        }
    }

    fun changeActivity() {
        finish()
    }
}

class GiphyResult(val data: GifData)
class GifData(val image_original_url: String)

