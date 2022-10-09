package io.github.irgaly.kottage.data.sqlite

import com.squareup.sqldelight.TransacterImpl
import com.squareup.sqldelight.db.SqlDriver

class KottageDatabase1(
    driver: SqlDriver
) : TransacterImpl(driver) {
    object Schema : SqlDriver.Schema {
        override val version: Int = 1
        override fun create(driver: SqlDriver) {
            driver.execute(
                null, """
          |CREATE TABLE item_stats (
          |  item_type TEXT PRIMARY KEY,
          |  count INTEGER NOT NULL DEFAULT 0,
          |  event_count INTEGER NOT NULL DEFAULT 0
          |)
          """.trimMargin(), 0
            )
            driver.execute(
                null, """
          |CREATE TABLE item_event (
          |  id TEXT PRIMARY KEY,
          |  created_at INTEGER NOT NULL,
          |  expire_at INTEGER,
          |  item_type TEXT NOT NULL,
          |  item_key TEXT NOT NULL,
          |  event_type TEXT NOT NULL
          |)
          """.trimMargin(), 0
            )
            driver.execute(
                null, """
          |CREATE TABLE stats (
          |  key TEXT PRIMARY KEY,
          |  last_evict_at INTEGER NOT NULL
          |)
          """.trimMargin(), 0
            )
            driver.execute(
                null, """
          |CREATE TABLE item (
          |  -- "{type}+{key}"
          |  key TEXT PRIMARY KEY,
          |  type TEXT NOT NULL,
          |  string_value TEXT,
          |  long_value INTEGER,
          |  double_value REAL,
          |  bytes_value BLOB,
          |  created_at INTEGER NOT NULL,
          |  last_read_at INTEGER NOT NULL,
          |  expire_at INTEGER
          |)
          """.trimMargin(), 0
            )
            driver.execute(null, "CREATE INDEX item_event_created_at ON item_event(created_at)", 0)
            driver.execute(null, "CREATE INDEX item_event_expire_at ON item_event(expire_at)", 0)
            driver.execute(
                null,
                "CREATE INDEX item_event_item_type_created_at ON item_event(item_type, created_at)",
                0
            )
            driver.execute(
                null,
                "CREATE INDEX item_event_item_type_expire_at ON item_event(item_type, expire_at)", 0
            )
            driver.execute(null, "CREATE INDEX item_type_created_at ON item(type, created_at)", 0)
            driver.execute(null, "CREATE INDEX item_type_last_read ON item(type, last_read_at)", 0)
            driver.execute(null, "CREATE INDEX item_type_expire_at ON item(type, expire_at)", 0)
        }

        override fun migrate(
            driver: SqlDriver,
            oldVersion: Int,
            newVersion: Int
        ) {
            KottageDatabase.Schema.migrate(driver, oldVersion, newVersion)
        }
    }
}
