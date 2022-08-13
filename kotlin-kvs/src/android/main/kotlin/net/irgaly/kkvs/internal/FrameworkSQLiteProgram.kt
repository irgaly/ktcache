/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.irgaly.kkvs.internal

import android.database.sqlite.SQLiteProgram
import androidx.sqlite.db.SupportSQLiteProgram

/**
 * An wrapper around [SQLiteProgram] to implement [SupportSQLiteProgram] API.
 */
internal open class FrameworkSQLiteProgram(private val mDelegate: SQLiteProgram) :
    SupportSQLiteProgram {
    override fun bindNull(index: Int) {
        mDelegate.bindNull(index)
    }

    override fun bindLong(index: Int, value: Long) {
        mDelegate.bindLong(index, value)
    }

    override fun bindDouble(index: Int, value: Double) {
        mDelegate.bindDouble(index, value)
    }

    override fun bindString(index: Int, value: String) {
        mDelegate.bindString(index, value)
    }

    override fun bindBlob(index: Int, value: ByteArray) {
        mDelegate.bindBlob(index, value)
    }

    override fun clearBindings() {
        mDelegate.clearBindings()
    }

    override fun close() {
        mDelegate.close()
    }
}
