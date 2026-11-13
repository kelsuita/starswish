package com.kelsuita.starswish.sync

/**
 * A failure that is shown to the user, so it carries an i18n key instead of a fixed sentence.
 * Anything else that goes wrong is logged and reported with a generic message.
 */
internal class SyncError(val key: String, val arg: String? = null, cause: Throwable? = null) :
    Exception("$key${arg?.let { ": $it" } ?: ""}", cause)
