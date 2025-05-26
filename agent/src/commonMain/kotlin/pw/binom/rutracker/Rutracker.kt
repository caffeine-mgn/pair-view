package pw.binom.rutracker

import pw.binom.charset.Charsets
import pw.binom.http.client.Http11ClientExchange
import pw.binom.http.client.HttpClientRunnable
import pw.binom.io.bufferedReader
import pw.binom.io.http.Headers
import pw.binom.io.http.HttpContentLength
import pw.binom.io.http.forEachHeader
import pw.binom.io.http.httpContentLength
import pw.binom.io.useAsync
import pw.binom.url.Query
import pw.binom.url.toURL
import pw.binom.xml.XmlParser
import pw.binom.xml.dom.XElement

class Rutracker(
    val client: HttpClientRunnable,
    val cookie: String = "bb_guid=dooEhOz28hZ1; bb_ssl=1; bb_session=0-7906186-wMojw4k81dF7vUuSDscp; bb_t=a%3A8%3A%7Bi%3A6692541%3Bi%3A1747222466%3Bi%3A6692433%3Bi%3A1747167543%3Bi%3A6682121%3Bi%3A1745345289%3Bi%3A6500155%3Bi%3A1743688601%3Bi%3A6670377%3Bi%3A1743618083%3Bi%3A6668672%3Bi%3A1743142598%3Bi%3A6666061%3Bi%3A1742983291%3Bi%3A6612608%3Bi%3A1738425668%3B%7D; cf_clearance=Onp84NOxVzP7Q7rLlMai1nMyprd0vsITeUHR5ilN2uA-1747847012-1.2.1.1-gTqxiVlTCNutgn5qqdgDkyUmWCbmGmDtlODCRxK.483ReEFd3Bst4_d6s6wUQW9N0k2p_9naVytZm4w7DRsUQDykkAE7ZM_6UYxAu0CAu7mBfpSzG2c84xCOjpWWEv0sQFTO7IS0KJIvQ90qK.u6cqoZc.DzzIN9MxYKQjkwjf2VooaDkOSwMVwnI0KiYZXdNX04dIxvDMXKq6O7PSFZ6PKOiJWwNZQxcedqhJolp7uyP8G7vp.qNonvHVYDPIKIjswu9GOoYJwYId3kutMsMByL3U7vys93DXeBwjSFgrPOi0YJGkoY__4puBRfYSo5JJLiQi3T3V7Gyq5zUKBtMEhEernLlCuYQJoI78QbP2w",
) {
    companion object {
        private const val BASE_URL = "https://rutracker.org/forum/tracker.php"
    }

    enum class Order {
        ASC,
        DESC,
    }

    private val Order.code
        get() = when (this) {
            Order.ASC -> "1"
            Order.DESC -> "2"
        }

    @JvmInline
    value class ForumId(val id: String) {
        companion object {
            fun fromUrl(url: String): ForumId {
                val index = url.indexOf("f=")
                check(index > 0) { "Invalid URL: \"$url\"" }
                return ForumId(url.substring(index + 2))
            }
        }
    }

    @JvmInline
    value class TopicId(val id: String) {
        companion object {
            fun fromUrl(url: String): TopicId {
                val index = url.indexOf("t=")
                check(index > 0) { "Invalid URL: \"$url\"" }
                return TopicId(url.substring(index + 2))
            }
        }
    }

    data class SearchResult(
        val categoryName: String,
        val categoryTopic: ForumId,
        val productName: String,
        val productTopic: TopicId,
        val size: String,
    )

    sealed interface Page {
        class Search(
            val results: List<SearchResult>,
            val searchId: String,
        ) : Page
    }

    private fun parsePage(text: String): Page {
        val searchId = text.lineSequence().find { "PG_BASE_URL: " in it }?.trim()
            ?.removePrefix("PG_BASE_URL: 'tracker.php?search_id=")
            ?.removeSuffix("',")

        var table = text.lineSequence()
            .dropWhile { "<table class=\"forumline tablesorter\" id=\"tor-tbl\">" !in it }
            .joinToString("\n")
        val e = table.indexOf("</table>")
        table = table.substring(0, e + 8)

        val gg = StringBuilder(table)
        gg.findAndClose("input")
        gg.findAndClose("br")
        gg.findAndClose("img")
        gg.removeBetween("<!--", "-->")

        val tableElement = XmlParser.parse(gg.toString())
            .asSequence()
            .filterIsInstance<XElement.Tag>()
            .filter { it.name == "table" }
            .first()
        val tableBody =
            tableElement.child.filterIsInstance<XElement.Tag>().find { it.name == "tbody" }!!
        val trs = tableBody.tags()
            .withName("tr")
        val files = trs
            .map {
                println(it)
                val tds = it.tags()
                    .withName("td")
                    .drop(2)
                    .toList()
                val topicDiv = tds[0].tags().withName("div").single()
                val topicLink = topicDiv.tags().withName("a").single()
                val topicHref = topicLink.attributes["href"]!!
                val topicText = topicLink.child.joinToString("")


                val fileDiv = tds[1].tags().withName("div").first()
                val fileLink = fileDiv.tags().withName("a").single()
                val fileHref = fileLink.attributes["href"]!!
                val fileText = fileLink.child.joinToString("").trim()

                val size = tds[3].tags().withName("a").single()
                    .child.joinToString("")
                    .replace("&nbsp;", "")
                    .replace("&#8595;", "")
                    .trim()

                SearchResult(
                    categoryName = topicText,
                    categoryTopic = ForumId.fromUrl(topicHref),
                    productName = fileText,
                    productTopic = TopicId.fromUrl(fileHref),
                    size = size,
                )
            }
            .toList()

        return Page.Search(
            results = files,
            searchId = searchId!!
        )
    }

    suspend fun search(
        text: String,
        order: Order = Order.DESC,
        s: SearchBy = SearchBy.REG,
        searchByTopics: List<String> = emptyList(),
    ) {
        var body: Query
        var query: Query
        if (s == SearchBy.REG && searchByTopics.isEmpty() && order == Order.DESC) {
            body = Query.new("max", "1").append("nm", text)
            query = Query.new("nm", text)
        } else {
            query = Query.new("nm", text)
            body = Query.EMPTY
            if (searchByTopics.isEmpty()) {
                body = body.append("f", "-1")
            } else {
                query = query.append("f", searchByTopics.joinToString(","))
                searchByTopics.forEach {
                    body = body.append("f", it)
                }
            }
            body = body.append("o", s.code)
            body = body.append("s", order.code)
            body = body.append("pn", "")
            body = body.append("nm", text)
        }


        val req = client.request(
            method = "POST",
            url = BASE_URL.toURL().copy(query = query)
        )
        req.headers.contentType = "application/x-www-form-urlencoded"
        req.headers.httpContentLength =
            HttpContentLength.CHUNKED
//            HttpContentLength.Fixed(size = body.raw.length.toULong(), chunked = false)
        req.headers[Headers.COOKIE] = cookie
        req.headers[Headers.ORIGIN] = "https://rutracker.org"
        req.headers[Headers.USER_AGENT] =
            "Mozilla/5.0 (X11; Linux x86_64; rv:138.0) Gecko/20100101 Firefox/138.0"
        req.headers["Referer"] = "https://rutracker.org/forum/tracker.php?nm=tools"
        req.headers["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        req.headers["Accept-Language"] = "ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3"
        println("Request!")
        req.headers.forEachHeader { key, value ->
            println("$key: $value")
        }
        val con = req.connect() as Http11ClientExchange
        val text = con.useAsync { connection ->
            connection.sendText(body.raw)
            val charSet = connection.getResponseHeaders().charset
            connection.getResponseHeaders().forEachHeader { key, value ->
                println("$key: $value")
            }
            println("ResponseCode: ${connection.getResponseCode()}")
            println("charSet=$charSet")
            connection.getInput()
                .bufferedReader(charset = Charsets.get("Windows-1251")).useAsync {
                    it.readText()
                }
        }


        val r = parsePage(text) as Page.Search


        r.results.forEach {
            println(it)
        }
    }
}

fun XElement.Tag.tags() = child.asSequence().filterIsInstance<XElement.Tag>()
fun Sequence<XElement.Tag>.withName(name: String) =
    filter { it.name == name }

val XElement.asTagOrNull
    get() = this as? XElement.Tag

fun StringBuilder.removeBetween(start: String, end: String) {
    var index = 0
    while (true) {
        val p = indexOf(start, index)
        if (p == -1) {
            break
        }
        val end1 = indexOf(end, p + start.length)
        if (end1 == -1) {
            break
        }
        delete(p, end1 + end.length)
        index = p + 1
    }
}

fun StringBuilder.findAndClose(tagName: String) {
    var start = 0
    while (true) {
        val p = indexOf("<$tagName", start)
        if (p == -1) {
            break
        }
        val end = indexOf('>', p + tagName.length)
        insert(end, '/')
        start = end
    }
}

/**
 * Сортировка: кол сидов
 * Направление: по убыванию
 * f%5B%5D=-1&o=10&s=2&pn=&nm=tools
 */

/**
 * Сортировка: кол сидов
 * Направление: по возростанию
 * f%5B%5D=-1&o=10&s=1&pn=&nm=tools
 */

enum class SearchBy(val code: String) {
    /**
     * Кол-во сидов
     */
    CIDE("10"),

    /**
     * Размер
     */
    SIZE("7"),

    /**
     * Дата регистрации
     */
    REG("1"),

    /**
     * Название темы
     */
    TOPIC_NAME("2"),

    /**
     * Кол-во скачиваний
     */
    DOWNLOADS("4"),

    /**
     * Кол-во личей
     */
    LICH("11"),
    ;

    val isDefault
        get() = this === REG
}
