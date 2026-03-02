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

data class StellarUiState(val notes: List<Note> = emptyList(), val query: String = "", val searchResults: List<SearchResult> = emptyList(), val focusedNoteId: Long? = null)
data class SearchResult(val note: Note, val score: Double, val normalizedScore: Float = 0f)

class StellarViewModel(private val repo: NoteRepository) : ViewModel() {
    private val query = MutableStateFlow(""); private val focusNoteId = MutableStateFlow<Long?>(null)
    val uiState: StateFlow<StellarUiState> = combine(repo.observeNotes(), query, focusNoteId) { notes, q, focus ->
        StellarUiState(notes, q, rankNotes(notes, q), focus)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StellarUiState())
    fun onQueryChange(v: String) { query.value = v }
    fun focusOn(id: Long?) { focusNoteId.value = id }
    fun addOrUpdate(old: Note?, title: String, content: String, starred: Boolean) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch { val now = System.currentTimeMillis(); val t = title.ifBlank { "\u65e0\u6807\u9898" }
            if (old == null) repo.upsert(Note(title = t, content = content, createdAt = now, updatedAt = now, starred = starred))
            else repo.upsert(old.copy(title = t, content = content, updatedAt = now, starred = starred)) }
    }
    fun toggleStar(n: Note) { viewModelScope.launch { repo.upsert(n.copy(starred = !n.starred, updatedAt = System.currentTimeMillis())) } }
    fun deleteNote(n: Note) { viewModelScope.launch { repo.delete(n) } }
    private fun rankNotes(notes: List<Note>, q: String): List<SearchResult> {
        if (q.isBlank()) return emptyList(); val tokens = q.lowercase().trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        val raw = notes.map { n -> val t = n.title.lowercase(); val c = n.content.lowercase(); var score = 0.0
            for (k in tokens) { if (t == k) score += 120.0; if (t.startsWith(k)) score += 65.0; if (t.contains(k)) score += 36.0; if (c.contains(k)) score += 18.0; score += fuzzy(k, t) * 24.0 + fuzzy(k, c) * 10.0 }
            if (n.starred) score += 12.0; score += max(0.0, 14.0 - (System.currentTimeMillis() - n.updatedAt).toDouble() / 86400000.0); SearchResult(n, score)
        }.filter { it.score > 5.0 }.sortedByDescending { it.score }; val mx = raw.maxOfOrNull { it.score } ?: 1.0
        return raw.map { it.copy(normalizedScore = (it.score / mx).toFloat().coerceIn(0.1f, 1f)) }
    }
    private fun fuzzy(a: String, b: String): Double { if (a.isBlank() || b.isBlank()) return 0.0; val dp = IntArray(b.length + 1); var best = 0
        for (i in a.indices) { var prev = 0; for (j in 1..b.length) { val tmp = dp[j]; dp[j] = if (a[i] == b[j-1]) prev + 1 else 0; if (dp[j] > best) best = dp[j]; prev = tmp } }; return best.toDouble() / a.length.coerceAtLeast(1) }
}
@Suppress("UNCHECKED_CAST")
class StellarViewModelFactory(private val repo: NoteRepository) : ViewModelProvider.Factory { override fun <T : ViewModel> create(modelClass: Class<T>): T = StellarViewModel(repo) as T }
