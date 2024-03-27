package hu.mbhbank.accountservice

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ActiveProfiles("test")
class AccountControllerTests(@Autowired private val mockMvc: MockMvc) {

    @Order(1)
    @Test
    fun `get accounts should return empty, when db is empty`() {
        val result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/account"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value(`is`(emptyList<Any>())))
                .andReturn()
        //.andExpect(jsonPath("$").value(emptyList<Any>())) // this fails with null, if db is empty
        // println(result.response.contentAsString)
    }

    @Order(3)
    @Test
    fun `get accounts should return all non deleted accounts`() {
        val result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/account"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value(`is`(emptyList<Any>())))
                .andReturn()
        //.andExpect(jsonPath("$").value(emptyList<Any>())) // this fails with null, if db is empty
        // println(result.response.contentAsString)
    }
}
