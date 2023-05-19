package tech.checkstory.payment.data.google_play

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials
import tech.checkstory.payment.domain.model.SubscriptionPurchaseWrapper
import java.io.FileInputStream

class SubscriptionsGoogleApi {

    private val jsonPath = "checkstory-940fc-6593b2b68c13.json"
    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val jsonFactory = JacksonFactory.getDefaultInstance()

    private val credentials = ServiceAccountCredentials.fromStream(FileInputStream(jsonPath))
    private val credentialAdapter = HttpCredentialsAdapter(credentials)

    private val publisher = AndroidPublisher.Builder(httpTransport, jsonFactory, credentialAdapter)
        .setApplicationName("Checkstory")
        .build()

    fun getSubscription(purchaseToken: String): SubscriptionPurchaseWrapper {
        val packageName = "dev.szymonchaber.checkstory"

        val subscription = publisher.purchases()
            .subscriptionsv2()
            .get(packageName, purchaseToken)
            .execute()
        return SubscriptionPurchaseWrapper(subscription)
    }
}