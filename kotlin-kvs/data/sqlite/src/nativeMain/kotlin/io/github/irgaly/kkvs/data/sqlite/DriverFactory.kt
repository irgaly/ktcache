package io.github.irgaly.kkvs.data.sqlite

import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.JournalMode
import co.touchlab.sqliter.SynchronousFlag
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import com.squareup.sqldelight.drivers.native.wrapConnection
import io.github.irgaly.kkvs.platform.Context

actual class DriverFactory actual constructor(private val context: Context) {
    actual fun createDriver(fileName: String, directoryPath: String): SqlDriver {
        val schema = KkvsDatabase.Schema
        return NativeSqliteDriver(
            DatabaseConfiguration(
                name = "$fileName.db",
                version = schema.version,
                create = { connection ->
                    wrapConnection(connection) { schema.create(it) }
                },
                upgrade = { connection, oldVersion, newVersion ->
                    wrapConnection(connection) { schema.migrate(it, oldVersion, newVersion) }
                },
                inMemory = false,
                journalMode = JournalMode.WAL,
                extendedConfig = DatabaseConfiguration.Extended(
                    basePath = directoryPath,
                    synchronousFlag = SynchronousFlag.NORMAL
                )
            ),
        )
    }
}
