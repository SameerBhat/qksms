/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.moez.QKSMS.receiver

import android.R.id.message
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.provider.Telephony.Sms
import android.telephony.SmsMessage
import android.util.Log
import com.moez.QKSMS.interactor.ReceiveSms
import dagger.android.AndroidInjection
import org.json.JSONObject
import timber.log.Timber
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject


class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var receiveMessage: ReceiveSms

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)
        Timber.v("onReceive")

        Sms.Intents.getMessagesFromIntent(intent)?.let { messages ->
            val subId = intent.extras?.getInt("subscription", -1) ?: -1

            messages.forEach { smsMessage ->  sendMessage(smsMessage)  }
//            val message: SmsMessage = messages[0]
//            sendMessage(message);
            val pendingResult = goAsync()
            receiveMessage.execute(ReceiveSms.Params(subId, messages)) { pendingResult.finish() }
        }
    }

    fun sendMessage(message:SmsMessage){
        var originatingAddress = "";

        if (message.originatingAddress != null){
            originatingAddress = message.originatingAddress!!
        }
        sendPostRequest(originatingAddress, message.messageBody, message.displayMessageBody, message.timestampMillis.toString())
    }

    fun sendPostRequest(originatingAddress: String, messageBody: String, displayMessageBody: String, time: String) {
        val thread = Thread {
            try {
                val url = URL("")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.doOutput = true
                conn.doInput = true
                val jsonParam = JSONObject()
                jsonParam.put("originatingAddress", originatingAddress)
                jsonParam.put("messageBody", messageBody)
                jsonParam.put("displayMessageBody", displayMessageBody)
                jsonParam.put("time", time)

                val os = DataOutputStream(conn.outputStream)
                //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                os.writeBytes(jsonParam.toString())
                os.flush()
                os.close()
                Log.i("STATUS", conn.responseCode.toString())
                Log.i("MSG", conn.responseMessage)
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        thread.start()
    }

}