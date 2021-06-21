package me.luke.edge.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelUuid
import android.widget.TextView
import androidx.lifecycle.Observer
import me.luke.edge.EdgeLiveData

class MainActivity2 : AppCompatActivity() {

    private lateinit var data2: EdgeLiveData<ParcelUuid>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        val text: TextView = findViewById(R.id.text)
        data2 = EdgeLiveData(this, R.id.text)
        data2.observe(this, Observer { text.text = it.uuid.toString() })
    }
}