package com.wpcodify.otprelay

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val DEFAULT_SERVER = "https://sms.narottamtours.com"

    private lateinit var serverUrl: EditText
    private lateinit var pairId: EditText
    private lateinit var pairId2: EditText
    private lateinit var device: EditText
    private lateinit var status: TextView

    private val permLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serverUrl = findViewById(R.id.serverUrl)
        pairId = findViewById(R.id.pairId)
        pairId2 = findViewById(R.id.pairId2)
        device = findViewById(R.id.device)
        status = findViewById(R.id.status)

        val p = Prefs.get(this)
        serverUrl.setText(p.getString("serverUrl", DEFAULT_SERVER))
        pairId.setText(p.getString("pairId", ""))
        pairId2.setText(p.getString("pairId2", ""))
        device.setText(p.getString("device", Build.MODEL))

        findViewById<Button>(R.id.save).setOnClickListener { save() }
        findViewById<Button>(R.id.grant).setOnClickListener { requestPerms() }
        findViewById<Button>(R.id.test).setOnClickListener { sendTest() }

        requestPerms()
    }

    private fun requestPerms() {
        val perms = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        permLauncher.launch(perms.toTypedArray())
    }

    private fun save() {
        Prefs.get(this).edit()
            .putString("serverUrl", serverUrl.text.toString().trim().trimEnd('/').ifBlank { DEFAULT_SERVER })
            .putString("pairId", pairId.text.toString().trim())
            .putString("pairId2", pairId2.text.toString().trim())
            .putString("device", device.text.toString().trim())
            .apply()
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
    }

    private fun sendTest() {
        save()
        val url = serverUrl.text.toString().trim().trimEnd('/').ifBlank { DEFAULT_SERVER }
        val pair = pairId.text.toString().trim()
        if (pair.isBlank()) { status.text = "Enter the Pair ID first."; return }
        status.text = "Sending test OTP…"
        Thread {
            val res = Net.ingest(url, mapOf(
                "pair_id" to pair,
                "raw" to "TEST Nine-Eight-One-Four-Eight-Five",
                "sim_slot" to "0", "device" to device.text.toString().trim()
            ))
            runOnUiThread { status.text = res }
        }.start()
    }
}
