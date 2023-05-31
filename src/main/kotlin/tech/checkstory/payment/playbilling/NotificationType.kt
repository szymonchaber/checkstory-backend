package tech.checkstory.payment.playbilling

enum class NotificationType(val intValue: Int) {
    SUBSCRIPTION_RECOVERED(1),
    SUBSCRIPTION_RENEWED(2),
    SUBSCRIPTION_CANCELED(3),
    SUBSCRIPTION_PURCHASED(4),
    SUBSCRIPTION_ON_HOLD(5),
    SUBSCRIPTION_IN_GRACE_PERIOD(6),
    SUBSCRIPTION_RESTARTED(7),
}