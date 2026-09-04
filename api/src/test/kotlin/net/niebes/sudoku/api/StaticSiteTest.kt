package net.niebes.sudoku.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

/**
 * The site lives in the web module and reaches the server only via the classpath - this pins that
 * wiring, so a rename in either module fails a test rather than quietly serving 404s.
 */
@SpringBootTest
@AutoConfigureMockMvc
internal class StaticSiteTest {

    @Autowired lateinit var mockMvc: MockMvc

    @Test
    fun servesTheSiteFromTheWebModule() {
        // Spring answers "/" by forwarding to the welcome page; MockMvc records the forward
        // rather than following it, so the content is asserted on the page itself.
        assertThat(mockMvc.perform(get("/")).andReturn().response.forwardedUrl).isEqualTo("index.html")

        val index = mockMvc.perform(get("/index.html")).andReturn().response
        assertThat(index.status).isEqualTo(200)
        assertThat(index.contentAsString).contains("Watch a Sudoku being solved")

        listOf("/app.js", "/techniques.js", "/style.css").forEach { asset ->
            assertThat(mockMvc.perform(get(asset)).andReturn().response.status)
                .describedAs(asset).isEqualTo(200)
        }
    }
}
