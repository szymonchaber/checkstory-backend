package tech.checkstory.payment.domain

import kotlinx.datetime.Instant

data class Purchase(val expirationDate: Instant, val googlePaymentToken: GooglePlayPaymentToken)
