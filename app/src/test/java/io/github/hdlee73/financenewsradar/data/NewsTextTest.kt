package io.github.hdlee73.financenewsradar.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsTextTest {
    @Test
    fun `normalizes publisher suffix and punctuation for duplicate detection`() {
        assertEquals(
            NewsText.normalizeTitle("[단독] 금감원, 증권사 내부통제 검사 - 연합뉴스"),
            NewsText.normalizeTitle("금감원 증권사 내부통제 검사 | 뉴스1")
        )
    }

    @Test
    fun `marks articles with multiple supervisory terms as priority`() {
        assertTrue(NewsText.isPriority("금감원, 증권사 검사 착수", "내부통제 실태를 살핀다"))
        assertFalse(NewsText.isPriority("증권사 신상품 출시", "새 펀드를 판매한다"))
    }

    @Test
    fun `returns matching watch keywords`() {
        assertEquals(
            listOf("증권사", "금융사고"),
            NewsText.matchingKeywords("증권사 금융사고", "금감원 검사", listOf("증권사", "자산운용사", "금융사고"))
        )
    }
}
