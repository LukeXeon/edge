package me.luke.edge.sample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.widget.TextView
import me.luke.edge.EdgeLiveData
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var data1: EdgeLiveData<ParcelUuid>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        data1 = EdgeLiveData(this, R.id.text)
        val handler = Handler(Looper.getMainLooper())
        Handler(Looper.getMainLooper()).post(object : Runnable {
            override fun run() {
                data1.setValue(ParcelUuid(UUID.randomUUID()))
                handler.postDelayed(this, 1000)
            }
        })
        startActivity(Intent(this, MainActivity2::class.java))
    }
}