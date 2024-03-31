package hu.mbhbank.accountservice.transactions.controller

import hu.mbhbank.accountservice.accounts.dao.AccountsRepository
import hu.mbhbank.accountservice.screening.ScreeningService
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@RestController
@RequestMapping("/api/v1/transaction")
class TransactionsController(
        private val transactionsRepository: TransactionsRepository,
        private val screeningService: ScreeningService,
        private val accountRepository: AccountsRepository
) {

    @GetMapping
    fun get(): List<Transaction> = transactionsRepository.findAll()

    @PostMapping
    fun create(@RequestBody transaction: Transaction): ResponseEntity<Transaction> {
        if (!screeningService.didAccountPassSecurityCheck(transaction.accountNumber)) {
            return ResponseEntity.notFound().build()
        }
        if (accountRepository.findByIsDeletedIsFalseAndAccountNumberEquals(transaction.accountNumber).isEmpty) {
            return ResponseEntity.notFound().build()
        }
        // Transaction can't be in the past - let's say 500 msec delta is acceptable:
        // When transaction timestamp is not submitted, then it's generated as a default value in the constructor
        // of `Transaction`.
        // It takes time, until the program execution reaches here.
        val currentTimeMinusDelta = LocalDateTime.now().minus(500, ChronoUnit.MILLIS)
        return if (transaction.timestamp!!.isBefore(currentTimeMinusDelta))
            ResponseEntity.badRequest().build()
        else
            ResponseEntity.ok(transactionsRepository.save(transaction.copy(uuid = UUID.randomUUID())))
    }

    @GetMapping("/{uuid}")
    fun getById(@PathVariable("uuid") uuid: UUID): Transaction {
        return transactionsRepository.findById(uuid).orElseThrow { NoSuchElementException("Transaction not found") }
    }

    @PutMapping("/{uuid}")
    fun update(@PathVariable("uuid") uuid: UUID, @RequestBody updatedTransaction: Transaction): Transaction {
        val existingTransaction = transactionsRepository.findById(uuid).orElseThrow { NoSuchElementException("Transaction not found") }
        val mergedTransaction = existingTransaction.copy(
                accountNumber = updatedTransaction.accountNumber,
                type = updatedTransaction.type,
                amount = updatedTransaction.amount,
                timestamp = updatedTransaction.timestamp
        )
        return transactionsRepository.save(mergedTransaction)
    }
}

@Entity(name = "transactions")
data class Transaction(
        @Id
        @GeneratedValue(generator = "UUID")
        @Column(columnDefinition = "uuid")
        val uuid: UUID?,
        val accountNumber: BigDecimal,
        @Enumerated(EnumType.ORDINAL)
        val type: Type,
        val amount: Long,
        val timestamp: LocalDateTime? = LocalDateTime.now()
) {}

enum class Type {
    DEPOSIT, WITHDRAWAL
}

interface TransactionsRepository : JpaRepository<Transaction, UUID> {
    fun findAllByAccountNumber(accountNumber: BigDecimal): List<Transaction>

    @Query(nativeQuery = true, value = """
        select t.* from transactions t
        join accounts a
        on t.account_number = t.account_number
        and a.is_deleted is False
        """)
    override fun findAll(): List<Transaction>
}