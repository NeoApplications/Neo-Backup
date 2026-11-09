package com.machiav3lli.backup.data.entity

import com.machiav3lli.backup.data.dbs.entity.Backup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class BackupsCache {
    private val backupsMap = ConcurrentHashMap<String, Set<Backup>>()
    private val mutex = Mutex()
    private val _updateTrigger = MutableStateFlow(0L)

    @Volatile
    var isInitialized: Boolean = false
        private set

    fun observeBackupsMap(): Flow<Map<String, Set<Backup>>> =
        _updateTrigger.asStateFlow().map { backupsMap.toMap() }

    fun observeBackupsList(): Flow<List<Backup>> =
        _updateTrigger.asStateFlow().map { backupsMap.values.flatten() }

    fun observeBackups(packageName: String): Flow<Set<Backup>> =
        _updateTrigger.asStateFlow().map { backupsMap[packageName] ?: emptySet() }

    fun getBackups(packageName: String): Set<Backup> =
        backupsMap[packageName] ?: emptySet()

    fun getBackupsMap(): Map<String, Set<Backup>> = backupsMap.toMap()

    fun getBackupsList(): List<Backup> = backupsMap.values.flatten()

    // SYNC SETTERS
    suspend fun updateBackups(packageName: String, backups: Set<Backup>) {
        mutex.withLock {
            backupsMap[packageName] = backups
            triggerUpdate()
        }
    }

    suspend fun replaceAll(backups: Map<String, Set<Backup>>) {
        mutex.withLock {
            backupsMap.clear()
            backupsMap.putAll(backups)
            isInitialized = true
            triggerUpdate()
        }
    }

    suspend fun remove(packageName: String) {
        mutex.withLock {
            backupsMap.remove(packageName)
            triggerUpdate()
        }
    }

    suspend fun removeAll(packageNames: List<String>) {
        mutex.withLock {
            packageNames.forEach { backupsMap.remove(it) }
            triggerUpdate()
        }
    }

    suspend fun clear() {
        mutex.withLock {
            backupsMap.clear()
            triggerUpdate()
        }
    }

    private fun triggerUpdate() {
        _updateTrigger.value = System.currentTimeMillis()
    }

    // SPECIAL GETTERS
    fun hasBackups(packageName: String): Boolean = backupsMap.containsKey(packageName)

    fun getBackupsCount(): Int = backupsMap.values.sumOf { it.size }

    fun getPackagesWithBackups(): Set<String> = backupsMap.keys
}