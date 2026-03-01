package com.hellostar.stellarnotes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hellostar.stellarnotes.data.Note
import com.hellostar.stellarnotes.data.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max

class StellarViewModel(private val repo: NoteRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val focusNoteId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<StellarUiState> = combine(
        repo.observeNotes(), query, focusNoteId
    ) { notes, q, focus ->
        StellarUiState(notes, q, rankNotes(notes, q), focus)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StellarUiState())

    fun onQueryChange(v: String) { query.value = v }
    fun focusOn(id: Long?) { focusNoteId.value = id }

    fun addOrUpdate(old: Note?, title: String, content: String, pinned: Boolean, starred: Boolean) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val n = if (old == null) {
                Note(title = title.ifBlank { "无标题" }, content = content, createdAt = now, updatedAt = now, pinned = pinned, starred = starred)
            } else {
                old.copy(title = title.ifBlank { "无标题" }, content = content, updatedAt = now, pinned = pinned, starred = starred)
            }
            repo.upsert(n)
        }
    }

    fun togglePin(n: Note) = viewModelScope.launch { repo.upsert(n.copy(pinned = !n.pinned, updatedAt = System.currentTimeMillis())) }
    fun toggleStar(n: Note) = viewModelScope.launch { repo.upsert(n.copy(starred = !n.starred, updatedAt = System.currentTimeMillis())) }
    fun delete(n: Note) = viewModelScope.launch { repo.delete(n) }

    private fun rankNotes(notes: List<Note>, q: String): List<SearchResult> {
        if (q.isBlank()) return notes.map { SearchResult(it, 0.0) }
        val tokens = q.lowercase().trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return notes.map { n ->
            val t = n.title.lowercase()
            val c = n.content.lowercase()
            var score = 0.0
            tokens.forEach { k ->
                if (t == k) score += 120
                if (t.startsWith(k)) score += 65
                if (t.contains(k)) score += 36
                if (c.contains(k)) score += 18
                score += fuzzy(k, t) * 24 + fuzzy(k, c) * 10
            }
            if (n.pinned) score += 12
            if (n.starred) score += 8
            score += max(0.0, 14 - (System.currentTimeMillis() - n.updatedAt) / 86_400_000.0)
            SearchResult(n, score)
        }.sortedByDescending { it.score }.filter { it.score > 8 }
    }

    private fun fuzzy(a: String, b: String): Double {
        if (a.isBlank() || b.isBlank()) return 0.0
        val dp = IntArray(b.length + 1)
        var best = 0
        for (i in a.indices) {
            var prev = 0
            for (j in 1..b.length) {
                val tmp = dp[j]
                dp[j] = if (a[i] == b[j - 1]) prev + 1 else 0
                best = max(best, dp[j])
                prev = tmp
            }
        }
        return best.toDouble() / a.length.coerceAtLeast(1)
    }
}

class StellarViewModelFactory(private val repo: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = StellarViewModel(repo) as T
}

data class StellarUiState(
    val notes: List<Note> = emptyList(),
    val query: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val focusedNoteId: Long? = null
)

data class SearchResult(val note: Note, val score: Double)
