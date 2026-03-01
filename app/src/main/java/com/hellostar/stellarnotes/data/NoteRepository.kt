package com.hellostar.stellarnotes.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val dao: NoteDao) {
    fun observeNotes(): Flow<List<Note>> = dao.observeAll()

    suspend fun upsert(note: Note) {
        if (note.id == 0L) dao.insert(note) else dao.update(note)
    }

    suspend fun delete(note: Note) = dao.delete(note)
}
