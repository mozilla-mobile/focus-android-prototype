/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.mozilla.focus.search.SearchEngine;
import org.mozilla.focus.search.SearchEngineManager;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.List;

public class UrlUtils {
    public static String normalize(@NonNull String input) {
        String trimmedInput = input.trim();
        Uri uri = Uri.parse(trimmedInput);

        if (TextUtils.isEmpty(uri.getScheme())) {
            uri = Uri.parse("http://" + trimmedInput);
        }

        return uri.toString();
    }

    /**
     * Is the given string a URL or should we perform a search?
     *
     * TODO: This is a super simple and probably stupid implementation.
     */
    public static boolean isUrl(String url) {
        String trimmedUrl = url.trim();
        if (trimmedUrl.contains(" ")) {
            return false;
        }

        return trimmedUrl.contains(".") || trimmedUrl.contains(":");
    }

    public static boolean isHttpOrHttps(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        return url.startsWith("http:") || url.startsWith("https:");
    }

    public static boolean isSearchQuery(String text) {
        return text.contains(" ");
    }

    public static String createSearchUrl(Context context, String searchTerm) {
        final SearchEngine searchEngine = SearchEngineManager.getInstance()
                .getDefaultSearchEngine(context);

        return searchEngine.buildSearchUrl(searchTerm);
    }

    /**
     * @return The search terms for the first search performed with the default search engine or the current URL.
     */
    public static String getSearchTermsOrUrl(String url) {
        String searchTermsOrUrl = null;
        final SearchEngine foundSearchEngine = UrlUtils.isDefaultSearchUrl(url);

        if (foundSearchEngine != null) {
            searchTermsOrUrl = UrlUtils.getSearchTermsFromUrl(url, foundSearchEngine);
        }

        if (TextUtils.isEmpty(searchTermsOrUrl)) {
            searchTermsOrUrl = url;
        }

        return searchTermsOrUrl;
    }

    /**
     * @return The detected search engine or null if one was not matched with the URL
     */
    private static SearchEngine isDefaultSearchUrl(String urlSearch) {
        if (TextUtils.isEmpty(urlSearch)) {
            return null;
        }

        final String representativeSnippet = UrlUtils.getRepresentativeSnippet(urlSearch);
        final List<SearchEngine> searchEngines = SearchEngineManager.getInstance().getSearchEngines();
        for (SearchEngine searchEngine : searchEngines) {
            if (representativeSnippet.contains(searchEngine.getName().toLowerCase())) {
                return searchEngine;
            }
        }
        return null;
    }

    /**
     * @return The search parameters decoded from the search url or null if they could not be extracted.
     */
    private static String getSearchTermsFromUrl(String url, SearchEngine searchEngine) {
        String searchParams = null;
        final Uri uri = Uri.parse(url);
        try {
            final String queryKey = searchEngine.getSearchTermsParamName();
            if (queryKey != null && url.contains(queryKey)) {
                searchParams = URLDecoder.decode(uri.getQueryParameter(queryKey), "UTF-8");
            }
        } catch (UnsupportedEncodingException exception) {
            searchParams = null;
        }
        return searchParams;
    }

    public static String stripUserInfo(@Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }

        try {
            URI uri = new URI(url);

            final String userInfo = uri.getUserInfo();
            if (userInfo == null) {
                return url;
            }

            // Strip the userInfo to minimise spoofing ability. This only affects what's shown
            // during browsing, this information isn't used when we start editing the URL:
            uri = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());

            return uri.toString();
        } catch (URISyntaxException e) {
            // We might be trying to display a user-entered URL (which could plausibly contain errors),
            // in this case its safe to just return the raw input.
            // There are also some special cases that URI can't handle, such as "http:" by itself.
            return url;
        }
    }

    public static boolean isPermittedResourceProtocol(@Nullable final String scheme) {
        return scheme != null && (
                scheme.startsWith("http") ||
                scheme.startsWith("https") ||
                scheme.startsWith("file") ||
                scheme.startsWith("data"));
    }

    public static boolean isSupportedProtocol(@Nullable final String scheme) {
        return scheme != null && (isPermittedResourceProtocol(scheme) || scheme.startsWith("error"));
    }

    public static boolean isInternalErrorURL(final String url) {
        return "data:text/html;charset=utf-8;base64,".equals(url);
    }

    public static boolean urlsMatchExceptForTrailingSlash(final @NonNull String url1, final @NonNull String url2) {
        int lengthDifference = url1.length() - url2.length();

        if (lengthDifference == 0) {
            // The simplest case:
            return url1.equalsIgnoreCase(url2);
        } else if (lengthDifference == 1) {
            // url1 is longer:
            return url1.charAt(url1.length() - 1) == '/' &&
                    url1.regionMatches(true, 0, url2, 0, url2.length());
        } else if (lengthDifference == -1) {
            return url2.charAt(url2.length() - 1) == '/' &&
                    url2.regionMatches(true, 0, url1, 0, url1.length());
        }

        return false;
    }

    public static String stripCommonSubdomains(@Nullable String host) {
        if (host == null) {
            return null;
        }

        // In contrast to desktop, we also strip mobile subdomains,
        // since its unlikely users are intentionally typing them
        int start = 0;

        if (host.startsWith("www.")) {
            start = 4;
        } else if (host.startsWith("mobile.")) {
            start = 7;
        } else if (host.startsWith("m.")) {
            start = 2;
        }

        return host.substring(start);
    }

    /**
     * Get the representative part of the URL. Usually this is the host (without common prefixes).
     */
    public static String getRepresentativeSnippet(@NonNull String url) {
        Uri uri = Uri.parse(url);

        // Use the host if available
        String snippet = uri.getHost();

        if (TextUtils.isEmpty(snippet)) {
            // If the uri does not have a host (e.g. file:// uris) then use the path
            snippet = uri.getPath();
        }

        if (TextUtils.isEmpty(snippet)) {
            // If we still have no snippet then just return the question mark
            return "?";
        }

        // Strip common prefixes that we do not want to use to determine the representative characterS
        snippet = UrlUtils.stripCommonSubdomains(snippet);

        return snippet;
    }
}
