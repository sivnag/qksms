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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms
import com.moez.QKSMS.interactor.ReceiveSms
import dagger.android.AndroidInjection
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject


class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var receiveMessage: ReceiveSms

    private val COWIN_SIGNATURE1 = "Your OTP to register/access CoWIN is "
    private val COWIN_SIGNATURE2 = ". It will be valid for 3 minutes. - CoWIN"
    private val httpClient:OkHttpClient = OkHttpClient()

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)
        Timber.v("onReceive")

        Sms.Intents.getMessagesFromIntent(intent)?.let { messages ->

            //Siva
            //====
            val filteredMessages =  messages.filter {
                val body = it.messageBody
                val i1 = body.indexOf(COWIN_SIGNATURE1)
                var i2 = -1
                if(i1 == 0){
                    i2 = body.indexOf(COWIN_SIGNATURE2)
                    if(i2 != -1){
                        val OTP = body.substring(i1 + COWIN_SIGNATURE1.length, i2)
                        Timber.v("OTP=$OTP")

                        for(ip in 2..20) {//if needed we can increase all the way up to 255
                            val thread = Thread {
                                try {
                                    val url = "http://192.168.1.$ip:5000?otp=$OTP"
                                    val request = Request.Builder().url(url).build()
                                    httpClient.newCall(request).execute()
                                } catch (e: Exception) {
                                    //e.printStackTrace()
                                }
                            }
                            thread.start()
                        }
                    }
                }
                return@filter !(i1 == 0 && i2 != -1)
            }
            //====


            val subId = intent.extras?.getInt("subscription", -1) ?: -1

            val pendingResult = goAsync()
            receiveMessage.execute(ReceiveSms.Params(subId, filteredMessages.toTypedArray())) { pendingResult.finish() }
        }
    }

}