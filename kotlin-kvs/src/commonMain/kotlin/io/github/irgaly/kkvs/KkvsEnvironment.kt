package io.github.irgaly.kkvs

import io.github.irgaly.kkvs.platform.Context
import io.github.irgaly.kkvs.platform.KkvsPlatformCalendar

/**
 * Runtime Environment
 */
data class KkvsEnvironment(
    val context: Context,
    val calendar: KkvsPlatformCalendar
)
