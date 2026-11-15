package com.kevin.searchstatemachine

/**
 * The search flow as a pure state machine: `reduce(state, event) -> state`.
 *
 * Side effects (the debounce timer and the fake network call) live in the UI and only
 * feed events back in. Each request carries an id, so a late debounce tick or a late
 * response for an older query is ignored instead of overwriting newer state.
 */
enum class SearchPhase { Idle, Suggest, Loading, Results, Empty, Error }

data class SearchUiState(
    val query: String = "",
    val phase: SearchPhase = SearchPhase.Idle,
    val suggestions: List<String> = emptyList(),
    val results: List<String> = emptyList(),
    /** Id of the request currently allowed to complete; bumped by every new search. */
    val requestId: Int = 0,
    /** Retries for the current query; lets the demo fail once and then succeed. */
    val attempt: Int = 0
)

sealed interface SearchEvent {
    data class QueryChanged(val query: String) : SearchEvent
    /** The debounce timer fired for [query]. */
    data class DebounceElapsed(val query: String) : SearchEvent
    /** A recent search or a suggestion was picked: search right away, no debounce. */
    data class Submitted(val query: String) : SearchEvent
    data class ResultsLoaded(val requestId: Int, val results: List<String>) : SearchEvent
    data class RequestFailed(val requestId: Int) : SearchEvent
    data object Retry : SearchEvent
    data object Clear : SearchEvent
}

const val MIN_QUERY_LENGTH = 3
const val DEBOUNCE_MILLIS = 350L

fun reduce(state: SearchUiState, event: SearchEvent): SearchUiState = when (event) {
    is SearchEvent.QueryChanged -> {
        val query = event.query
        if (query.isBlank()) {
            SearchUiState(requestId = state.requestId + 1)
        } else {
            // Typing cancels any request in flight (new id) and shows suggestions until
            // the debounce fires.
            state.copy(
                query = query,
                phase = SearchPhase.Suggest,
                suggestions = suggestionsFor(query),
                results = emptyList(),
                requestId = state.requestId + 1,
                attempt = 0
            )
        }
    }

    is SearchEvent.DebounceElapsed ->
        if (state.phase != SearchPhase.Suggest || event.query != state.query ||
            event.query.trim().length < MIN_QUERY_LENGTH
        ) state
        else state.startLoading(attempt = 0)

    is SearchEvent.Submitted ->
        if (event.query.isBlank()) state
        else state.copy(query = event.query).startLoading(attempt = 0)

    is SearchEvent.ResultsLoaded ->
        if (event.requestId != state.requestId || state.phase != SearchPhase.Loading) state
        else state.copy(
            phase = if (event.results.isEmpty()) SearchPhase.Empty else SearchPhase.Results,
            results = event.results
        )

    is SearchEvent.RequestFailed ->
        if (event.requestId != state.requestId || state.phase != SearchPhase.Loading) state
        else state.copy(phase = SearchPhase.Error, results = emptyList())

    SearchEvent.Retry ->
        if (state.phase != SearchPhase.Error) state
        else state.startLoading(attempt = state.attempt + 1)

    SearchEvent.Clear -> SearchUiState(requestId = state.requestId + 1)
}

private fun SearchUiState.startLoading(attempt: Int) = copy(
    phase = SearchPhase.Loading,
    suggestions = emptyList(),
    results = emptyList(),
    requestId = requestId + 1,
    attempt = attempt
)

val recentSearches = listOf("budget split", "compose snackbar", "offline sync", "kmp storage")

private val suggestionPool = listOf(
    "budget app", "budget alert", "budget categories", "budget forecast",
    "compose snackbar", "compose navigation", "offline sync", "kmp storage", "portfolio review"
)

private val catalog = listOf(
    "Budget forecast", "Monthly budget rules", "Savings plan", "Budget split with friends",
    "Compose snackbar recipes", "Offline sync queue", "KMP storage options", "Portfolio review checklist"
)

fun suggestionsFor(query: String): List<String> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    return suggestionPool.filter { it.startsWith(q) || it.split(' ').any { word -> word.startsWith(q) } }.take(4)
}

/** Fake backend: queries mentioning "portfolio" fail on the first attempt so Retry can be shown. */
fun fakeSearch(query: String, attempt: Int): Result<List<String>> {
    val q = query.trim().lowercase()
    if ("portfolio" in q && attempt == 0) return Result.failure(IllegalStateException("Network timeout"))
    val words = q.split(' ').filter { it.isNotBlank() }
    return Result.success(catalog.filter { title -> words.all { title.lowercase().contains(it) } })
}
