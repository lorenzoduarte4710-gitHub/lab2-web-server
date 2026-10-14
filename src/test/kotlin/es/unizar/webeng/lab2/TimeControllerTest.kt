package es.unizar.webeng.lab2

import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

/** Checks `GET /time` through MockMvc: JSON shape, the `zone` parameter and invalid zones. */
@SpringBootTest
@AutoConfigureMockMvc
class TimeControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun timeIsJson() {
        mockMvc
            .perform(get("/time").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.time").exists())
            .andExpect(jsonPath("$.zone").value(ZoneId.systemDefault().id))
    }

    @Test
    fun timeUsesRequestedZone() {
        // Tokyo is several hours away from the server zone, so a wrong zone would show.
        val tokyo = ZoneId.of("Asia/Tokyo")
        val body =
            mockMvc
                .perform(get("/time").param("zone", tokyo.id))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.zone").value(tokyo.id))
                .andReturn()
                .response.contentAsString
        val time = LocalDateTime.parse(JsonPath.read<String>(body, "$.time"))
        val gap = Duration.between(time, LocalDateTime.now(tokyo)).abs()
        assertTrue(gap < Duration.ofMinutes(1), "time should be the current time in Asia/Tokyo, gap was $gap")
    }

    @Test
    fun unknownZoneIsBadRequest() {
        mockMvc
            .perform(get("/time").param("zone", "Not/AZone"))
            .andExpect(status().isBadRequest)
    }
}
