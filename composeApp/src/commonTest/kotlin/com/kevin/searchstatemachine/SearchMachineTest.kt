package com.kevin.searchstatemachine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SearchMachineTest {

    private fun SearchUiState.after(vararg events: SearchEvent) = events.fold(this) { s, e -> reduce(s, e) }

    @Test
    fun typingShowsSuggestionsAndWaitsForDebounce() {
        val s = SearchUiState().after(SearchEvent.QueryChanged("bud"))
        assertEquals(SearchPhase.Suggest, s.phase)
        assertTrue(s.suggestions.all { it.startsWith("bud") })
        assertTrue(s.suggestions.isNotEmpty())
    }

    @Test
    fun debounceStartsLoadingOnlyForLatestQuery() {
        val typed = SearchUiState().after(SearchEvent.QueryChanged("bud"), SearchEvent.QueryChanged("budget"))
        // A timer started for an older keystroke must not trigger a search.
        assertSame(typed, reduce(typed, SearchEvent.DebounceElapsed("bud")))
        val loading = reduce(typed, SearchEvent.DebounceElapsed("budget"))
        assertEquals(SearchPhase.Loading, loading.phase)
        assertEquals("budget", loading.query)
    }

    @Test
    fun debounceIgnoresQueriesThatAreTooShort() {
        val s = SearchUiState().after(SearchEvent.QueryChanged("bu"))
        assertSame(s, reduce(s, SearchEvent.DebounceElapsed("bu")))
    }

    @Test
    fun lateDebounceDoesNotRestartASubmittedSearch() {
        val typed = SearchUiState().after(SearchEvent.QueryChanged("budget"))
        val submitted = reduce(typed, SearchEvent.Submitted("budget"))
        assertSame(submitted, reduce(submitted, SearchEvent.DebounceElapsed("budget")))
    }

    @Test
    fun resultsAndEmptyAreSplitOnResultCount() {
        val loading = SearchUiState().after(SearchEvent.Submitted("budget"))
        val results = reduce(loading, SearchEvent.ResultsLoaded(loading.requestId, listOf("Budget forecast")))
        assertEquals(SearchPhase.Results, results.phase)
        val empty = reduce(loading, SearchEvent.ResultsLoaded(loading.requestId, emptyList()))
        assertEquals(SearchPhase.Empty, empty.phase)
    }

    @Test
    fun staleResponseIsIgnoredAfterQueryChanges() {
        val first = SearchUiState().after(SearchEvent.Submitted("budget"))
        val typing = reduce(first, SearchEvent.QueryChanged("budget s"))
        val after = reduce(typing, SearchEvent.ResultsLoaded(first.requestId, listOf("Budget forecast")))
        assertSame(typing, after)
    }

    @Test
    fun errorKeepsQueryAndRetryStartsANewRequest() {
        val loading = SearchUiState().after(SearchEvent.Submitted("portfolio"))
        val error = reduce(loading, SearchEvent.RequestFailed(loading.requestId))
        assertEquals(SearchPhase.Error, error.phase)
        assertEquals("portfolio", error.query)
        val retry = reduce(error, SearchEvent.Retry)
        assertEquals(SearchPhase.Loading, retry.phase)
        assertEquals(1, retry.attempt)
        assertTrue(retry.requestId > error.requestId)
    }

    @Test
    fun clearReturnsToIdleAndDropsInFlightRequest() {
        val loading = SearchUiState().after(SearchEvent.Submitted("budget"))
        val cleared = reduce(loading, SearchEvent.Clear)
        assertEquals(SearchPhase.Idle, cleared.phase)
        assertEquals("", cleared.query)
        assertSame(cleared, reduce(cleared, SearchEvent.ResultsLoaded(loading.requestId, listOf("x"))))
    }

    @Test
    fun fakeSearchFailsPortfolioOnceThenSucceeds() {
        assertTrue(fakeSearch("portfolio", attempt = 0).isFailure)
        assertEquals(listOf("Portfolio review checklist"), fakeSearch("portfolio", attempt = 1).getOrThrow())
        assertEquals(emptyList(), fakeSearch("crypto taxes", attempt = 0).getOrThrow())
    }
}
