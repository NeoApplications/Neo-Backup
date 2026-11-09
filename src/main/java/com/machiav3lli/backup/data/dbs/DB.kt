/*
 * Neo Backup: open-source apps backup and restore app.
 * Copyright (C) 2020  Antonios Hazim
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.machiav3lli.backup.data.dbs

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import com.machiav3lli.backup.MAIN_DB_NAME
import com.machiav3lli.backup.data.dbs.dao.AppExtrasDao
import com.machiav3lli.backup.data.dbs.dao.AppInfoDao
import com.machiav3lli.backup.data.dbs.dao.BlocklistDao
import com.machiav3lli.backup.data.dbs.dao.ScheduleDao
import com.machiav3lli.backup.data.dbs.entity.AppExtras
import com.machiav3lli.backup.data.dbs.entity.AppInfo
import com.machiav3lli.backup.data.dbs.entity.Blocklist
import com.machiav3lli.backup.data.dbs.entity.ExtrasTags
import com.machiav3lli.backup.data.dbs.entity.Schedule
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

@Database(
    entities = [
        Schedule::class,
        Blocklist::class,
        AppExtras::class,
        AppInfo::class,
    ],
    version = 8333,
    exportSchema = true,
    views = [
        ExtrasTags::class,
    ],
    autoMigrations = [
        AutoMigration(from = 2, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6, spec = DB.Companion.AutoMigration5to6::class),
        AutoMigration(from = 6, to = 7, spec = DB.Companion.AutoMigration6to7::class),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 8328),
        AutoMigration(from = 8328, to = 8330),
        AutoMigration(from = 8330, to = 8332, spec = DB.Companion.AutoMigration8330to8332::class),
        AutoMigration(from = 8332, to = 8333, spec = DB.Companion.AutoMigration8332to8333::class),
    ]
)
@TypeConverters(Converters::class)
abstract class DB : RoomDatabase() {
    abstract fun getScheduleDao(): ScheduleDao
    abstract fun getBlocklistDao(): BlocklistDao
    abstract fun getAppExtrasDao(): AppExtrasDao
    abstract fun getAppInfoDao(): AppInfoDao

    companion object {
        @Volatile
        private var INSTANCE: DB? = null

        fun getInstance(context: Context): DB {
            synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = Room
                        .databaseBuilder(
                            context.applicationContext, DB::class.java,
                            MAIN_DB_NAME
                        )
                        .fallbackToDestructiveMigration(false)
                        .build()
                }
                return INSTANCE!!
            }
        }

        @DeleteColumn(
            tableName = "AppExtras",
            columnName = "id"
        )
        class AutoMigration5to6 : AutoMigrationSpec

        @DeleteColumn(
            tableName = "Schedule",
            columnName = "specialFilter"
        )
        class AutoMigration6to7 : AutoMigrationSpec

        @DeleteTable(tableName = "SpecialInfo")
        class AutoMigration8330to8332 : AutoMigrationSpec

        @DeleteTable(tableName = "Backup")
        class AutoMigration8332to8333 : AutoMigrationSpec
    }
}

val databaseModule = module {
    single { DB.getInstance(androidContext()) }
    single { get<DB>().getScheduleDao() }
    single { get<DB>().getBlocklistDao() }
    single { get<DB>().getAppExtrasDao() }
    single { get<DB>().getAppInfoDao() }
}
