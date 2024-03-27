package hu.mbhbank.accountservice

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.test.context.ActiveProfiles

private const val ACCOUNT_URL = "/api/v1/account"

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ActiveProfiles("test")
class AccountControllerTests(@Autowired private val mockMvc: MockMvc) {

    @Order(1)
    @Test
    fun `get accounts should return empty, when db is empty`() {
        val result = mockMvc.perform(get(ACCOUNT_URL))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value(`is`(emptyList<Any>())))
                .andReturn()
        //.andExpect(jsonPath("$").value(emptyList<Any>())) // this fails with null, if db is empty
        // println(result.response.contentAsString)
    }

    @Order(2)
    @Test
    fun `creating new account should return the account`() {
        mockMvc.perform(post(ACCOUNT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountHolderName\":\"John Doe\"}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.accountHolderName").value("John Doe"))
                .andExpect(jsonPath("$.accountNumber").isNumber)

        mockMvc.perform(post(ACCOUNT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountHolderName\":\"Jane Doe\"}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.accountHolderName").value("Jane Doe"))
                .andExpect(jsonPath("$.accountNumber").isNumber)
    }

    @Order(3)
    @Test
    fun `get accounts should return all non deleted accounts`() {
        mockMvc.perform(get(ACCOUNT_URL))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].accountHolderName").value("John Doe"))
                .andExpect(jsonPath("$[0].accountNumber").isNumber)
                .andExpect(jsonPath("$[1].accountHolderName").value("Jane Doe"))
                .andExpect(jsonPath("$[1].accountNumber").isNumber)
    }
}
