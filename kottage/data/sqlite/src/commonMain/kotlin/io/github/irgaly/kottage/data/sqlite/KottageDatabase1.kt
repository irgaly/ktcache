package io.github.irgaly.kottage.data.sqlite

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.SqlDriver

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
            driver.execute(
                null,
                "CREATE INDEX item_type_last_read_at ON item(type, last_read_at)",
                0
            )
            driver.execute(null, "CREATE INDEX item_type_expire_at ON item(type, expire_at)", 0)
            driver.execute(
                null,
                "INSERT INTO item_stats(item_type, count, event_count) VALUES('cache1', 1, 1)",
                0
            )
            driver.execute(
                null,
                // 1640995200000 = 2022/1/1 00:00:00.000 UTC
                // 1643587200000 = 2022/1/31 00:00:00.000 UTC
                "INSERT INTO item_event(id, created_at, expire_at, item_type, item_key, event_type) VALUES('61b658c4250a4e9a9d07a9815655c5e1', 1640995200000, 1643587200000, 'cache1', 'cache1+key1', 'Create')",
                0
            )
            driver.execute(
                null,
                // 1640995200000 = 2022/1/1 00:00:00.000 UTC
                "INSERT INTO stats(key, last_evict_at) VALUES('kottage', 1640995200000)",
                0
            )
            driver.execute(
                null,
                // 1640995200000 = 2022/1/1 00:00:00.000 UTC
                // 1643587200000 = 2022/1/31 00:00:00.000 UTC
                "INSERT INTO item(key, type, string_value, long_value, double_value, bytes_value, created_at, last_read_at, expire_at) VALUES('cache1+key1', 'cache1', 'value1', NULL, NULL, NULL, 1640995200000, 1640995200000, 1643587200000)",
                0
            )
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
