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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.UUID


private const val TRANSACTION_URL = "/api/v1/transaction"

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class TransactionControllerTests(
        @Autowired private val mockMvc: MockMvc,
        @Autowired
        private val jdbc: JdbcTemplate
) {

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
                .content("""{"accountNumber": "12345678111111111", "type": "DEPOSIT", "amount": 100}"""))
                .andExpect(status().isNotFound)
    }

    @Order(3)
    @Test
    fun `can't create transactions for account, that did not pass security checks`() {
        val response = mockMvc.perform(post(ACCOUNT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"accountHolderName":"John Doe"}"""))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.accountHolderName").value("John Doe"))
                .andExpect(jsonPath("$.accountNumber").isNumber)
                .andReturn().response.contentAsString

        accountId = JsonPath.parse(response).read<BigInteger>("$.accountNumber")

        mockMvc.perform(post(TRANSACTION_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"accountNumber": $accountId, "type": "DEPOSIT", "amount": 100}"""))
                .andExpect(status().isNotFound)
    }

    @Order(4)
    @Test
    fun `create new transaction with valid account`() { // TODO: Implement validity checking for account
        // fake external validation
        jdbc.update("insert into security_response values ($accountId, true)")

        val transactionResponse = mockMvc.perform(post(TRANSACTION_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"accountNumber": $accountId, "type": "DEPOSIT", "amount": 100}"""))
                .andExpect(status().isOk)
                .andReturn().response.contentAsString

        transactionId = UUID.fromString(JsonPath.parse(transactionResponse).read<String>("$.uuid"))
        println(transactionId)
    }

    @Order(5)
    @Test
    fun `get balance for account returns with correct amount`() {
        mockMvc.perform(get("$ACCOUNT_URL/$accountId/balance"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value("100"))

        mockMvc.perform(post(TRANSACTION_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"accountNumber": $accountId, "type": "DEPOSIT", "amount": 50}"""))

        mockMvc.perform(post(TRANSACTION_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"accountNumber": $accountId, "type": "WITHDRAWAL", "amount": 10}"""))

        mockMvc.perform(get("$ACCOUNT_URL/$accountId/balance"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value("140"))
    }

    @Order(6)
    @Test
    fun `transactions with past timestamp should be rejected`() {
        val pastTimestamp = LocalDateTime.now().minusHours(1).toString()
        val transactionString = """
            {
            "accountNumber": $accountId,
            "type": "DEPOSIT",
            "amount": 50,
            "timestamp":"$pastTimestamp"
            }
        """.trimIndent()
        mockMvc.perform(post(TRANSACTION_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transactionString))
                .andExpect(status().is4xxClientError)
    }

    @Order(7)
    @Test
    fun `transactions with future timestamp should be allowed`() {
        val futureTimestamp = LocalDateTime.now().plusHours(1).toString()
        val transactionString = """
            {
            "accountNumber": $accountId,
            "type": "DEPOSIT",
            "amount": 50,
            "timestamp":"$futureTimestamp"
            }
        """.trimIndent()
        val futureTimestampTruncated = futureTimestamp.substring(0, 27) // TODO: this can be out of bounds
        mockMvc.perform(post(TRANSACTION_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transactionString))
                .andExpect(status().isOk)
                // returning timestamp missing 2 digits: 2024-03-29T23:55:39.020385400 vs 2024-03-29T23:55:39.0203854
                .andExpect(jsonPath("$.timestamp").value(futureTimestampTruncated))
    }
}
