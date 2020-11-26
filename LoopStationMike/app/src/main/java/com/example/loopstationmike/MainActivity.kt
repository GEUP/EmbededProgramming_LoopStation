package com.example.LoopStationMike

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.io.IOException


private const val LOG_TAG = "AudioRecordTest"
private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

class MainActivity : AppCompatActivity() {
    private lateinit var mqttAndroidClient: MqttAndroidClient
    private var fileName: String = ""
    private var recordButton: RecordButton? = null
    private var recorder: MediaRecorder? = null

    private var playButton: PlayButton? = null
    private var player: MediaPlayer? = null
    var poisition = 0
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToRecordAccepted) finish()
    }

    private fun onRecord(start: Boolean) = if (start) {
        startRecording()
    } else {
        stopRecording()
    }

    private fun onPlay(start: Boolean) = if (start) {
        startPlaying()
    } else {
        stopPlaying()
    }

    private fun startPlaying() {
        player = MediaPlayer().apply {
            try {
                setDataSource(fileName)
                prepare()
                start()
                Toast.makeText(applicationContext,fileName,Toast.LENGTH_LONG).show()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
                Toast.makeText(applicationContext,"prepare() failed",Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun stopPlaying() {
        player?.release()
        player = null
    }

    private fun startRecording() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }

            start()
        }
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
            Toast.makeText(applicationContext,fileName,Toast.LENGTH_LONG).show()
        }
        recorder = null
    }

    internal inner class RecordButton(ctx: Context) : androidx.appcompat.widget.AppCompatButton(ctx) {

        var mStartRecording = true

        var clicker: OnClickListener = OnClickListener {
            onRecord(mStartRecording)
            text = when (mStartRecording) {
                true -> "Stop recording"
                false -> "Start recording"
            }
            mStartRecording = !mStartRecording
        }

        init {
            text = "Start recording"
            setOnClickListener(clicker)
        }
    }

    internal inner class PlayButton(ctx: Context) : androidx.appcompat.widget.AppCompatButton(ctx) {
        var mStartPlaying = true
        var clicker: OnClickListener = OnClickListener {
            onPlay(mStartPlaying)
            text = when (mStartPlaying) {
                true -> "Stop playing"
                false -> "Start playing"
            }
            mStartPlaying = !mStartPlaying
        }

        init {
            text = "Start playing"
            setOnClickListener(clicker)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn.setOnClickListener {
            this.connect(this.applicationContext)
        }
        subtn.setOnClickListener {
            subscribe("CTRL-MIKE")
            receiveMessages()
        }
        fileName = "${externalCacheDir?.absolutePath}/audiorecordtest.mp3"

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)

        recordButton = RecordButton(this)
        playButton = PlayButton(this)
        val ll = LinearLayout(this).apply {
            addView(recordButton,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    0f))
            addView(playButton,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    0f))
        }
        setContentView(ll)

    }
    override fun onStop() {
        super.onStop()
        recorder?.release()
        recorder = null
        player?.release()
        player = null
    }

    private fun connect(applicationContext : Context) {
        try {
            mqttAndroidClient = MqttAndroidClient(applicationContext,"tcp://"+ev.text.toString()+":1883",MqttClient.generateClientId())
            var a = mqttAndroidClient.connect()
            Toast.makeText(applicationContext,"tcp://"+ev.text.toString()+":1883", Toast.LENGTH_SHORT).show()
                a.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken){
                    Log.i("Connection", "success ")
                    Toast.makeText(applicationContext,"Connection",Toast.LENGTH_LONG).show()
                    //connectionStatus = true
                    // Give your callback on connection established here
                }
                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    //connectionStatus = false
                    Log.i("Connection", "failure")
                    // Give your callback on connection failure here
                    Toast.makeText(applicationContext,"connect fail",Toast.LENGTH_SHORT).show()
                    exception.printStackTrace()
                }
            }
        } catch (e: MqttException) {
            // Give your callback on connection failure here
            //e.printStackTrace()
            Toast.makeText(applicationContext,"Exception",Toast.LENGTH_SHORT).show()
        }

    }
    private fun subscribe(topic: String) {
        val qos = 2 // Mention your qos value
        try {
            mqttAndroidClient.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    // Give your callback on Subscription here
                    Toast.makeText(applicationContext,"subscribed",Toast.LENGTH_SHORT).show()
                }
                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {
                    // Give your subscription failure callback here
                    Toast.makeText(applicationContext,"fail",Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: MqttException) {
            // Give your subscription failure callback here
        }
    }
    fun unSubscribe(topic: String) {
        try {
            val unsubToken = mqttAndroidClient.unsubscribe(topic)
            unsubToken.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    // Give your callback on unsubscribing here
                }
                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    // Give your callback on failure here
                }
            }
        } catch (e: MqttException) {
            // Give your callback on failure here
        }
    }
    fun receiveMessages() {
        mqttAndroidClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable) {
                //connectionStatus = false
                // Give your callback on failure here
            }
            override fun messageArrived(topic: String, message: MqttMessage) {

                //Toast.makeText(applicationContext,"MESSAGE ARRIVED",Toast.LENGTH_SHORT).show()
                try {
                    val data = String(message.payload, charset("UTF-8"))
                    // data is the desired received message
                    // Give your callback on message received here
                    tv.text = message.toString()
                    if(tv.text.equals("MIKE ON"))
                    {
                        //Toast.makeText(applicationContext,"START MIKE",Toast.LENGTH_SHORT).show()
                    }
                    else if(tv.text.equals("MIKE OFF"))
                    {
                        //Toast.makeText(applicationContext,"STOP MIKE",Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    // Give your callback on error here
                }
            }
            override fun deliveryComplete(token: IMqttDeliveryToken) {
                // Acknowledgement on delivery complete
            }
        })
    }
    fun publish(topic: String, data: String) {
        val encodedPayload : ByteArray
        try {
            encodedPayload = data.toByteArray(charset("UTF-8"))
            val message = MqttMessage(encodedPayload)
            message.qos = 2
            message.isRetained = false
            mqttAndroidClient.publish(topic, message)
        } catch (e: Exception) {
            // Give Callback on error here
        } catch (e: MqttException) {
            // Give Callback on error here
        }
    }
    fun disconnect() {
        try {
            val disconToken = mqttAndroidClient.disconnect()
            disconToken.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    //connectionStatus = false
                    // Give Callback on disconnection here
                }
                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {
                    // Give Callback on error here
                }
            }
        } catch (e: MqttException) {
            // Give Callback on error here
        }
    }
}
