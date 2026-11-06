package io.github.kcbear.trading;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Purpose: integration test of the generated REST API - verifies the 12,000 seeded instruments
 * are pageable via the react-admin contract (Content-Range total, page size, enum filter).
 */
@SpringBootTest
@AutoConfigureMockMvc
class InstrumentApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void pagesThroughTwelveThousandInstruments() throws Exception {
        mvc.perform(get("/api/instruments").param("range", "[0,9]").param("sort", "[\"symbol\",\"ASC\"]"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Range", "instruments 0-9/12000"))
                .andExpect(jsonPath("$.length()").value(10));
    }

    @Test
    void filtersByInstrumentType() throws Exception {
        mvc.perform(get("/api/instruments").param("range", "[0,4]")
                        .param("filter", "{\"instrumentType\":\"ETF\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].instrumentType").value("ETF"));
    }
}
