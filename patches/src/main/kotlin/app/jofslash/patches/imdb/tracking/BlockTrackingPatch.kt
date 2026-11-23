package app.jofslash.patches.imdb.tracking

import app.jofslash.patches.imdb.shared.Constants.COMPATIBILITY_IMDB
import app.jofslash.patches.imdb.shared.methodOf
import app.jofslash.patches.imdb.shared.returnEarly
import app.jofslash.patches.imdb.shared.returnUnit
import app.morphe.patcher.patch.bytecodePatch

// comScore audience measurement: notifyStart() builds the PublisherConfiguration and calls
// Analytics.start(context).
private const val COMSCORE = "Lcom/imdb/mobile/metrics/Comscore;"

// Branch deep-link + install attribution: initSessionWhenSafe(CoroutineScope, Continuation) is the
// suspend entry that starts the Branch session.
private const val BRANCH_INJECTABLE = "Lcom/imdb/mobile/metrics/BranchInjectable;"

// AWS Pinpoint. firstLaunch() reports the first-launch analytics event; reportSubscriptionMetrics()
// reports push-subscription metrics. enableDefaultTopicSubscriptions() is left alone so push topics
// (and push itself) keep working.
private const val PINPOINT_EVENT_COORDINATOR = "Lcom/imdb/mobile/notifications/PinpointEventCoordinator;"
private const val PINPOINT_COORDINATOR = "Lcom/imdb/mobile/notifications/PinpointCoordinator;"

// Amazon clickstream: ClickstreamTimer.start() starts the periodic Timer that flushes the
// clickstream buffer.
private const val CLICKSTREAM_TIMER = "Lcom/imdb/mobile/imdbloggingsystem/tr/ClickstreamTimer;"

// Play install referrer -> clickstream: InstallReferrerRetriever.retrieve() connects to the install
// referrer service and sends the referrer.
private const val INSTALL_REFERRER_RETRIEVER = "Lcom/imdb/mobile/metrics/InstallReferrerRetriever;"

@Suppress("unused")
val blockTrackingPatch = bytecodePatch(
    name = "Block tracking",
    description = "Stops the third-party trackers: comScore audience measurement, Branch attribution, the AWS " +
            "Pinpoint analytics events, the Amazon clickstream timer and the Play install-referrer lookup. Push " +
            "notifications and their topic subscriptions are kept. Browsing, search and your watchlist aren't touched.",
    default = true
) {
    category("Privacy")
    compatibleWith(COMPATIBILITY_IMDB)

    execute {
        // comScore: nothing is registered or started.
        methodOf(COMSCORE, "notifyStart").returnEarly()

        // Branch: the suspend session-init returns Unit at once, so no Branch session starts.
        methodOf(BRANCH_INJECTABLE, "initSessionWhenSafe").returnUnit()

        // Pinpoint analytics: the first-launch event and the subscription-metrics report are not
        // sent. enableDefaultTopicSubscriptions() is deliberately untouched so push survives.
        methodOf(PINPOINT_EVENT_COORDINATOR, "firstLaunch").returnEarly()
        methodOf(PINPOINT_COORDINATOR, "reportSubscriptionMetrics").returnEarly()

        // Amazon clickstream timer: the periodic flush never starts.
        methodOf(CLICKSTREAM_TIMER, "start").returnEarly()

        // Install referrer: the Play referrer is never read or sent to the clickstream.
        methodOf(INSTALL_REFERRER_RETRIEVER, "retrieve").returnEarly()
    }
}
