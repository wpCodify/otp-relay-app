package com.wpcodify.otprelay

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

class SmsReceiver : BroadcastReceiver() {

    companion object {
        // In-process de-dupe: both SIMs / multi-part / retries of the same OTP within 15s -> send once.
        private var lastOtp: String? = null
        private var lastTime: Long = 0
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val body = StringBuilder()
        for (sms in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
            body.append(sms.messageBody ?: "")
        }
        val text = body.toString()
        if (text.isBlank()) return

        val otp = Otp.extract(text) ?: return  // ignore non-OTP messages

        val now = System.currentTimeMillis()
        synchronized(SmsReceiver) {
            if (otp == lastOtp && now - lastTime < 15000) return
            lastOtp = otp; lastTime = now
        }

        val prefs = Prefs.get(context)
        val serverUrl = (prefs.getString("serverUrl", "") ?: "").ifBlank { "https://sms.narottamtours.com" }

        val slot = simSlot(context, intent)                 // 0 = SIM1, 1 = SIM2
        // One Pair ID for the phone; optional separate ID for SIM 2.
        val main = prefs.getString("pairId", "") ?: ""
        val sim2 = prefs.getString("pairId2", "") ?: ""
        val pair = if (slot == 1 && sim2.isNotBlank()) sim2 else main
        if (pair.isBlank()) return

        val device = prefs.getString("device", "") ?: android.os.Build.MODEL

        val pending = goAsync()
        Thread {
            try {
                Net.ingest(serverUrl, mapOf(
                    "pair_id" to pair,
                    "otp" to otp,
                    "raw" to text.take(200),
                    "sim_slot" to slot.toString(),
                    "device" to device
                ))
            } finally { pending.finish() }
        }.start()
    }

    private fun simSlot(context: Context, intent: Intent): Int {
        val subId = intent.extras?.let {
            when {
                it.containsKey("subscription") -> it.getInt("subscription", -1)
                it.containsKey("android.telephony.extra.SUBSCRIPTION_INDEX") ->
                    it.getInt("android.telephony.extra.SUBSCRIPTION_INDEX", -1)
                else -> -1
            }
        } ?: -1
        if (subId < 0) return 0
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) return 0
        return try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            sm.getActiveSubscriptionInfo(subId)?.simSlotIndex ?: 0
        } catch (e: Exception) { 0 }
    }
}
