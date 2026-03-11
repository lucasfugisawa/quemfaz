package com.fugisawa.quemfaz.core.pagination

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PageRequestTest {
    @Test
    fun `create valid page request should succeed`() {
        val request = PageRequest(limit = 20, offset = 0)
        assertEquals(20, request.limit)
        assertEquals(0, request.offset)
    }

    @Test
    fun `create page request with zero limit should fail`() {
        assertFailsWith<IllegalArgumentException> {
            PageRequest(limit = 0, offset = 0)
        }
    }

    @Test
    fun `create page request with negative limit should fail`() {
        assertFailsWith<IllegalArgumentException> {
            PageRequest(limit = -1, offset = 0)
        }
    }

    @Test
    fun `create page request exceeding max limit should fail`() {
        assertFailsWith<IllegalArgumentException> {
            PageRequest(limit = 101, offset = 0)
        }
    }

    @Test
    fun `create page request with negative offset should fail`() {
        assertFailsWith<IllegalArgumentException> {
            PageRequest(limit = 20, offset = -1)
        }
    }

    @Test
    fun `default page request should have correct values`() {
        val request = PageRequest.default()
        assertEquals(20, request.limit)
        assertEquals(0, request.offset)
    }

    @Test
    fun `first page should have zero offset`() {
        val request = PageRequest.firstPage(30)
        assertEquals(30, request.limit)
        assertEquals(0, request.offset)
        assertTrue(request.isFirstPage)
    }

    @Test
    fun `next page should calculate correct offset`() {
        val first = PageRequest(limit = 20, offset = 0)
        val second = PageRequest.nextPage(first)
        assertEquals(20, second.limit)
        assertEquals(20, second.offset)
    }

    @Test
    fun `page number should calculate correctly`() {
        assertEquals(1, PageRequest(20, 0).pageNumber)
        assertEquals(2, PageRequest(20, 20).pageNumber)
        assertEquals(3, PageRequest(20, 40).pageNumber)
    }

    @Test
    fun `isFirstPage should be correct`() {
        assertTrue(PageRequest(20, 0).isFirstPage)
        assertFalse(PageRequest(20, 20).isFirstPage)
    }
}

class PageTest {
    @Test
    fun `page with more results should have hasMore true`() {
        val page = Page(items = List(20) { it }, limit = 20, offset = 0, total = 100)
        assertTrue(page.hasMore)
        assertFalse(page.isLastPage)
    }

    @Test
    fun `page with no more results should have hasMore false`() {
        val page = Page(items = List(20) { it }, limit = 20, offset = 80, total = 100)
        assertFalse(page.hasMore)
        assertTrue(page.isLastPage)
    }

    @Test
    fun `first page should have isFirstPage true`() {
        val page = Page(items = List(20) { it }, limit = 20, offset = 0, total = 100)
        assertTrue(page.isFirstPage)
    }

    @Test
    fun `non-first page should have isFirstPage false`() {
        val page = Page(items = List(20) { it }, limit = 20, offset = 20, total = 100)
        assertFalse(page.isFirstPage)
    }

    @Test
    fun `total pages should calculate correctly`() {
        assertEquals(5, Page<String>(emptyList(), 20, 0, 100).totalPages)
        assertEquals(3, Page<String>(emptyList(), 20, 0, 50).totalPages)
        assertEquals(1, Page<String>(emptyList(), 20, 0, 15).totalPages)
    }

    @Test
    fun `page number should calculate correctly`() {
        assertEquals(1, Page<String>(emptyList(), 20, 0, 100).pageNumber)
        assertEquals(2, Page<String>(emptyList(), 20, 20, 100).pageNumber)
        assertEquals(5, Page<String>(emptyList(), 20, 80, 100).pageNumber)
    }

    @Test
    fun `empty page should work correctly`() {
        val request = PageRequest(limit = 20, offset = 0)
        val page = Page.empty<String>(request)
        assertEquals(0, page.items.size)
        assertEquals(20, page.limit)
        assertEquals(0, page.offset)
        assertEquals(0, page.total)
        assertFalse(page.hasMore)
    }

    @Test
    fun `map should transform items correctly`() {
        val page = Page(items = listOf(1, 2, 3), limit = 10, offset = 0, total = 3)
        val mapped = page.map { it * 2 }
        assertEquals(listOf(2, 4, 6), mapped.items)
        assertEquals(10, mapped.limit)
        assertEquals(0, mapped.offset)
        assertEquals(3, mapped.total)
    }
}
