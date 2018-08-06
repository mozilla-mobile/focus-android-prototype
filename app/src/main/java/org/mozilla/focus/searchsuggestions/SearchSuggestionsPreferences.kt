package org.mozilla.focus.searchsuggestions

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.content.res.Resources
import android.preference.PreferenceManager
import org.mozilla.focus.R
import mozilla.components.browser.search.SearchEngine
import org.mozilla.focus.Components
import org.mozilla.focus.utils.Settings

class SearchSuggestionsPreferences(private val context: Context) {
    private val settings = Settings.getInstance(context)
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun searchSuggestionsEnabled(): Boolean = settings.shouldShowSearchSuggestions()
    fun hasUserToggledSearchSuggestions(): Boolean = settings.userHasToggledSearchSuggestions()

    fun getSearchEngine(): SearchEngine {
        return Components.searchEngineManager.getDefaultSearchEngine(
                context, settings.defaultSearchEngineName)
    }

    fun enableSearchSuggestions() {
        preferences.edit()
                .putBoolean(context.resources.getString(R.string.pref_key_user_toggled_search_suggestions), true)
                .putBoolean(context.resources.getString(R.string.pref_key_show_search_suggestions), true)
                .apply()
    }

    fun disableSearchSuggestions() {
        preferences.edit()
                .putBoolean(context.resources.getString(R.string.pref_key_user_toggled_search_suggestions), true)
                .putBoolean(context.resources.getString(R.string.pref_key_show_search_suggestions), false)
                .apply()
    }
}
