package io.github.hdlee73.financenewsradar.data

import io.github.hdlee73.financenewsradar.model.AppSettings
import io.github.hdlee73.financenewsradar.model.NaverCredentials
import io.github.hdlee73.financenewsradar.model.NewsArticle
import io.github.hdlee73.financenewsradar.model.NewsProviderType
import io.github.hdlee73.financenewsradar.model.OutletScope
import io.github.hdlee73.financenewsradar.model.SearchPage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class NewsRepository {
    suspend fun search(
        query: String,
        settings: AppSettings,
        credentials: NaverCredentials,
        start: Int = 1
    ): SearchPage {
        val provider = provider(settings.provider, credentials)
        val page = provider.search(query, settings.timeRange, start)
        return page.copy(articles = refine(page.articles, settings, listOf(query)))
    }

    suspend fun home(settings: AppSettings, credentials: NaverCredentials): SearchPage = coroutineScope {
        val provider = provider(settings.provider, credentials)
        val pages = settings.keywords
            .filter { it.isNotBlank() }
            .map { keyword -> async { provider.search(expandFinanceQuery(keyword), settings.timeRange, pageSize = 40) } }
            .awaitAll()
        SearchPage(refine(pages.flatMap { it.articles }, settings, settings.keywords))
    }

    private fun provider(type: NewsProviderType, credentials: NaverCredentials): NewsProvider = when (type) {
        NewsProviderType.GOOGLE_RSS -> GoogleNewsRssProvider()
        NewsProviderType.NAVER -> NaverNewsProvider(credentials)
    }

    private fun refine(
        items: List<NewsArticle>,
        settings: AppSettings,
        keywords: List<String>
    ): List<NewsArticle> {
        val seenTitles = mutableSetOf<String>()
        return items.asSequence()
            .filter { settings.outletScope == OutletScope.ALL || PublisherCatalog.isMajor(it.source, it.link) }
            .sortedByDescending { it.publishedAt }
            .filter { article ->
                val key = NewsText.normalizeTitle(article.title).ifBlank { article.link }
                seenTitles.add(key)
            }
            .map { article ->
                article.copy(
                    matchedKeywords = NewsText.matchingKeywords(article.title, article.summary, keywords),
                    isPriority = NewsText.isPriority(article.title, article.summary)
                )
            }
            .toList()
    }

    private fun expandFinanceQuery(keyword: String): String = when (keyword.trim()) {
        "금융감독원", "금감원" -> "(금융감독원 OR 금감원) (증권 OR 자산운용 OR 금융투자 OR 펀드)"
        "자산운용사", "운용사" -> "(자산운용사 OR 운용사) (금감원 OR 금융감독 OR 펀드)"
        "증권사" -> "증권사 (금감원 OR 검사 OR 제재 OR 내부통제 OR 금융사고)"
        "금융사고" -> "금융사고 (증권사 OR 운용사 OR 금융투자)"
        else -> keyword
    }
}
