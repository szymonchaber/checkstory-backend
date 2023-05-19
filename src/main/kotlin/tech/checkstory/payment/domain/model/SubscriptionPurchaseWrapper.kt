package tech.checkstory.payment.domain.model

import com.google.api.services.androidpublisher.model.SubscriptionPurchaseV2

data class SubscriptionPurchaseWrapper(val rawValue: SubscriptionPurchaseV2) {

    val subscriptionState: SubscriptionState = when (rawValue.subscriptionState) {
        "SUBSCRIPTION_STATE_UNSPECIFIED" -> SubscriptionState.UNSPECIFIED
        "SUBSCRIPTION_STATE_PENDING" -> SubscriptionState.PENDING
        "SUBSCRIPTION_STATE_ACTIVE" -> SubscriptionState.ACTIVE
        "SUBSCRIPTION_STATE_PAUSED" -> SubscriptionState.PAUSED
        "SUBSCRIPTION_STATE_IN_GRACE_PERIOD" -> SubscriptionState.IN_GRACE_PERIOD
        "SUBSCRIPTION_STATE_ON_HOLD" -> SubscriptionState.ON_HOLD
        "SUBSCRIPTION_STATE_CANCELED" -> SubscriptionState.CANCELED
        "SUBSCRIPTION_STATE_EXPIRED" -> SubscriptionState.EXPIRED
        else -> SubscriptionState.UNSPECIFIED
    }

    enum class SubscriptionState {

        /**
         *  Unspecified subscription state.
         */
        UNSPECIFIED,

        /**
         *  Subscription was created but awaiting payment during signup. In this state, all items are awaiting payment.
         */
        PENDING,

        /**
         *  Subscription is active. - (1) If the subscription is an auto renewing plan, at least one item is autoRenewEnabled and not expired. - (2) If the subscription is a prepaid plan, at least one item is not expired.
         */
        ACTIVE,

        /**
         *  Subscription is paused. The state is only available when the subscription is an auto renewing plan. In this state, all items are in paused state.
         */
        PAUSED,

        /**
         *  Subscription is in grace period. The state is only available when the subscription is an auto renewing plan. In this state, all items are in grace period.
         */
        IN_GRACE_PERIOD,

        /**
         *  Subscription is on hold (suspended). The state is only available when the subscription is an auto renewing plan. In this state, all items are on hold.
         */
        ON_HOLD,

        /**
         *  Subscription is canceled but not expired yet. The state is only available when the subscription is an auto renewing plan. All items have autoRenewEnabled set to false.
         */
        CANCELED,

        /**
         * Subscription is expired. All items have expiryTime in the past.
         */
        EXPIRED;

        companion object {

            val SubscriptionState.shouldGrantAccess: Boolean
                get() {
                    return when (this) {
                        ACTIVE, IN_GRACE_PERIOD -> true
                        UNSPECIFIED,
                        PENDING,
                        PAUSED,
                        ON_HOLD,
                        CANCELED,
                        EXPIRED -> false
                    }
                }
        }
    }
}