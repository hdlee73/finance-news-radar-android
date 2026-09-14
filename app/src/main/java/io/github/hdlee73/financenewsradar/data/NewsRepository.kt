package io.github.hdlee73.financenewsradar.data

import io.github.hdlee73.financenewsradar.model.AppSettings
import io.github.hdlee73.financenewsradar.model.NaverCredentials
import io.github.hdlee73.financenewsradar.model.NewsArticle
import io.github.hdlee73.financenewsradar.model.NewsProviderType
import io.github.hdlee73.financenewsradar.model.OutletScope
import io.github.hdlee73.financenewsradar.model.SearchPage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

class NewsRepository {
    suspend fun search(
        query: String,
        settings: AppSettings,
        credentials: NaverCredentials,
        start: Int = 1
    ): SearchPage {
        val provider = provider(settings.provider, credentials)
        val page = provider.search(query, settings.timeRange, start)
        val refined = refine(page.articles, settings, listOf(query))
        return page.copy(
            articles = refined.articles,
            fetchedCount = page.articles.size,
            duplicateCount = refined.duplicateCount,
            outletExcludedCount = refined.outletExcludedCount
        )
    }

    suspend fun home(settings: AppSettings, credentials: NaverCredentials): SearchPage = supervisorScope {
        val provider = provider(settings.provider, credentials)
        val queries = settings.keywords
            .filter { it.isNotBlank() }
            .flatMap { keyword -> NewsQueryPlanner.homeQueries(keyword, settings.provider) }
            .distinct()
        val results = queries
            .map { query -> async { runCatching { provider.search(query, settings.timeRange, pageSize = 100) } } }
            .awaitAll()
        val pages = results.mapNotNull { it.getOrNull() }
        if (pages.isEmpty()) throw results.firstNotNullOfOrNull { it.exceptionOrNull() }
            ?: IllegalStateException("기사를 불러오지 못했습니다.")
        val fetched = pages.flatMap { it.articles }
        val refined = refine(fetched, settings, settings.keywords)
        SearchPage(
            articles = refined.articles,
            fetchedCount = fetched.size,
            duplicateCount = refined.duplicateCount,
            outletExcludedCount = refined.outletExcludedCount,
            failedQueryCount = results.count { it.isFailure }
        )
    }

    private fun provider(type: NewsProviderType, credentials: NaverCredentials): NewsProvider = when (type) {
        NewsProviderType.GOOGLE_RSS -> GoogleNewsRssProvider()
        NewsProviderType.NAVER -> NaverNewsProvider(credentials)
    }

    private data class RefinedArticles(
        val articles: List<NewsArticle>,
        val duplicateCount: Int,
        val outletExcludedCount: Int
    )

    private fun refine(
        items: List<NewsArticle>,
        settings: AppSettings,
        keywords: List<String>
    ): RefinedArticles {
        val inScope = items.filter {
            settings.outletScope == OutletScope.ALL || PublisherCatalog.isMajor(it.source, it.link)
        }
        val seenTitles = mutableSetOf<String>()
        val articles = inScope.asSequence()
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
        return RefinedArticles(
            articles = articles,
            duplicateCount = inScope.size - articles.size,
            outletExcludedCount = items.size - inScope.size
        )
    }

}

internal object NewsQueryPlanner {
    fun homeQueries(keyword: String, provider: NewsProviderType): List<String> {
        val exact = keyword.trim()
        if (provider != NewsProviderType.GOOGLE_RSS) return listOf(exact)
        return listOf(exact, expandFinanceQuery(exact)).distinct()
    }

    private fun expandFinanceQuery(keyword: String): String = when (keyword.trim()) {
        "금융감독원", "금감원" -> "(금융감독원 OR 금감원) (증권 OR 자산운용 OR 금융투자 OR 펀드)"
        "자산운용사", "운용사" -> "(자산운용사 OR 운용사) (금감원 OR 금융감독 OR 펀드)"
        "증권사" -> "증권사 (금감원 OR 검사 OR 제재 OR 내부통제 OR 금융사고)"
        "금융투자" -> "금융투자 (금감원 OR 검사 OR 제재 OR 내부통제 OR 금융사고)"
        "금융사고" -> "금융사고 (증권사 OR 운용사 OR 금융투자)"
        else -> keyword
    }
}
