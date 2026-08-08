package com.byteflipper.random.data.person

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeopleRepository @Inject constructor(
    private val dao: PersonDao
) {
    fun observeAll(): Flow<List<Person>> = dao.observeAll()

    suspend fun upsert(person: Person): Long = withContext(Dispatchers.IO) {
        dao.upsert(person)
    }

    suspend fun getById(id: Long): Person? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun archive(id: Long, updatedAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        dao.archive(id, updatedAt)
    }
}
