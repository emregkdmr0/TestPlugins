package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class ExampleProvider : MainAPI() {
    override var mainUrl = "https://www.movy.sx"
    override var name = "Movy"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    // 1. Ana Sayfa (Trendler / Son Eklenenler)
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val items = document.select("div.flw-item, div.film-poster").mapNotNull { element ->
            val title = element.selectFirst("h2.film-name, h3.film-name, a.dynamic-name")?.text() 
                ?: element.selectFirst("img")?.attr("alt") 
                ?: return@mapNotNull null
            val href = fixUrl(element.selectFirst("a")?.attr("href") ?: return@mapNotNull null)
            val poster = fixUrlNull(element.selectFirst("img")?.attr("data-src") 
                ?: element.selectFirst("img")?.attr("src"))

            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = poster
            }
        }
        return newHomePageResponse("Trending", items)
    }

    // 2. Arama
    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/search/${query.replace(" ", "-")}"
        val document = app.get(searchUrl).document

        return document.select("div.flw-item, div.film-poster").mapNotNull { element ->
            val title = element.selectFirst("h2.film-name, h3.film-name, a.dynamic-name")?.text() 
                ?: element.selectFirst("img")?.attr("alt") 
                ?: return@mapNotNull null
            val href = fixUrl(element.selectFirst("a")?.attr("href") ?: return@mapNotNull null)
            val poster = fixUrlNull(element.selectFirst("img")?.attr("data-src") 
                ?: element.selectFirst("img")?.attr("src"))

            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = poster
            }
        }
    }

    // 3. Detay Sayfası (Film/Dizi Bilgileri)
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h2.heading-name, h1")?.text()?.trim() ?: "Bilinmeyen Başlık"
        val poster = fixUrlNull(document.selectFirst("div.film-poster img, .miv-cover img")?.attr("src"))
        val description = document.selectFirst("div.description, .film-description")?.text()?.trim()

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    // 4. Video Oynatıcı Linklerini Çekme
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document

        // Sayfa içindeki oynatıcı iframe'lerini tarar ve çözer
        val iframes = document.select("iframe").mapNotNull { it.attr("src") }
        for (iframe in iframes) {
            val fullUrl = fixUrl(iframe)
            loadExtractor(fullUrl, subtitleCallback, callback)
        }
        return true
    }
}
