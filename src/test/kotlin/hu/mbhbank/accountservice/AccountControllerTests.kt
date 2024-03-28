package hu.mbhbank.accountservice

import com.jayway.jsonpath.JsonPath
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.test.context.ActiveProfiles
import java.math.BigInteger

private const val ACCOUNT_URL = "/api/v1/account"

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ActiveProfiles("test")
class AccountControllerTests(@Autowired private val mockMvc: MockMvc) {

    @Autowired
    lateinit var jdbc: JdbcTemplate

    companion object {
        @JvmStatic
        lateinit var id1: BigInteger

        @JvmStatic
        lateinit var id2: BigInteger
    }


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
        val response = mockMvc.perform(get(ACCOUNT_URL))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].accountHolderName").value("John Doe"))
                .andExpect(jsonPath("$[0].accountNumber").isNumber)
                .andExpect(jsonPath("$[1].accountHolderName").value("Jane Doe"))
                .andExpect(jsonPath("$[1].accountNumber").isNumber)
                .andReturn().response.contentAsString

        id1 = JsonPath.parse(response).read<BigInteger>("$[0].accountNumber")
        id2 = JsonPath.parse(response).read<BigInteger>("$[1].accountNumber")
        val idToDelete = id2
        mockMvc.perform(delete("$ACCOUNT_URL/$idToDelete"))

        mockMvc.perform(get(ACCOUNT_URL))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].accountHolderName").value("John Doe"))
                .andExpect(jsonPath("$[0].accountNumber").isNumber)

        // check if delete does not really delete account from database
        val rows = jdbc.queryForObject<Long>("select count(*) from accounts where account_number = $idToDelete")
        assertEquals(1L, rows)
    }

    @Order(4)
    @Test
    fun `get accounts by id should return non deleted accounts, and not deleted accounts`() {
        mockMvc.perform(get("$ACCOUNT_URL/$id1"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountHolderName").value("John Doe"))

        mockMvc.perform(get("$ACCOUNT_URL/$id2"))
                .andExpect(status().isNotFound)
    }
}
