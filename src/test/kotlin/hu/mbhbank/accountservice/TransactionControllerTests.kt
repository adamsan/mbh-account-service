package hu.mbhbank.accountservice

import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.hamcrest.Matchers.*
import java.math.BigInteger
import java.util.UUID


private const val TRANSACTION_URL = "/api/v1/transaction"

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ActiveProfiles("test")
class TransactionControllerTests(@Autowired private val mockMvc: MockMvc) {

    companion object {
        @JvmStatic
        lateinit var accountId: BigInteger

        @JvmStatic
        lateinit var transactionId: UUID
    }


    @Order(1)
    @Test
    fun `get transactions should return empty when db is empty`() {
        mockMvc.perform(get(TRANSACTION_URL))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value(`is`(emptyList<Any>())))
    }

    @Order(2)
    @Test
    fun `can't create new transaction without valid account`() {
        mockMvc.perform(post(TRANSACTION_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountNumber\": \"12345678111111111\", \"type\": \"DEPOSIT\", \"amount\": 100}"))
                .andExpect(status().is5xxServerError)
    }

    @Order(3)
    @Test
    fun `create new transaction with valid account`() { // TODO: Implement validity checking for account
        val response = mockMvc.perform(post(ACCOUNT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountHolderName\":\"John Doe\"}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.accountHolderName").value("John Doe"))
                .andExpect(jsonPath("$.accountNumber").isNumber)
                .andReturn().response.contentAsString

        accountId = JsonPath.parse(response).read<BigInteger>("$.accountNumber")

        // TODO: perform external validation

        val transactionResponse = mockMvc.perform(post(TRANSACTION_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountNumber\": $accountId, \"type\": \"DEPOSIT\", \"amount\": 100}"))
                .andExpect(status().isOk)
                .andReturn().response.contentAsString

        transactionId = UUID.fromString(JsonPath.parse(transactionResponse).read<String>("$.uuid"))
        println(transactionId)
    }

    @Order(4)
    @Test
    fun `get balance for account returns with correct amount`() {
        mockMvc.perform(get("$ACCOUNT_URL/$accountId/balance"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value("100"))

        mockMvc.perform(post(TRANSACTION_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountNumber\": $accountId, \"type\": \"DEPOSIT\", \"amount\": 50}"))

        mockMvc.perform(post(TRANSACTION_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountNumber\": $accountId, \"type\": \"WITHDRAWAL\", \"amount\": 10}"))

        mockMvc.perform(get("$ACCOUNT_URL/$accountId/balance"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value("140"))

    }
}
