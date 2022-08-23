package io.github.irgaly.kkvs.data.sqlite

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import io.github.irgaly.kkvs.platform.Context
import java.util.*

actual class DriverFactory actual constructor(private val context: Context) {
    actual fun createDriver(fileName: String, directoryPath: String): SqlDriver {
        // SQLDelight + SQLiter + JDBC:
        // * sqlite_busy_timeout = 3000 (default)
        //   * https://github.com/xerial/sqlite-jdbc/blob/14d59032fb5dc691e48877ecde783719b7657fba/src/main/java/org/sqlite/SQLiteConfig.java
        // * threading mode = multi-thread
        //   * https://github.com/xerial/sqlite-jdbc/issues/199
        //   * ThreadedConnectionManager で ThreadLocal Connection
        //     * https://github.com/cashapp/sqldelight/blob/cb699fcc19632a70deeb2930470bcf09db625a42/drivers/sqlite-driver/src/main/kotlin/app/cash/sqldelight/driver/jdbc/sqlite/JdbcSqliteDriver.kt#L83
        //     * 単発クエリはどのスレッドからでも呼び出し可能
        //     * Transaction 開始~終了の間では同一スレッドである必要がある
        val driver = JdbcSqliteDriver(
            "jdbc:sqlite:${directoryPath}/$fileName.db",
            Properties().apply {
                put("journal_mode", "WAL")
                put("synchronous", "NORMAL")
                put("busy_timeout", "3000")
            }
        )
        KkvsDatabase.Schema.create(driver)
        return driver
    }
}
