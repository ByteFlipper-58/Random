package com.byteflipper.random.consent

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

class ConsentManager(private val context: Context) {

    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    fun requestConsent(
        activity: Activity,
        onReadyForAds: (canRequestAds: Boolean) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val params = ConsentRequestParameters.Builder()
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // После обновления — загружаем и показываем форму по требованию
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    activity
                ) { formError: FormError? ->
                    if (formError != null) {
                        onError("UMP form error: ${formError.errorCode} ${formError.message}")
                    }
                    onReadyForAds(consentInformation.canRequestAds())
                }
            },
            { formError: FormError ->
                onError("UMP update error: ${formError.errorCode} ${formError.message}")
                onReadyForAds(consentInformation.canRequestAds())
            }
        )
    }

    fun canRequestAds(): Boolean = consentInformation.canRequestAds()
}


