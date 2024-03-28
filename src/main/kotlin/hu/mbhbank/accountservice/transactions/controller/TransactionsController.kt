package hu.mbhbank.accountservice.transactions.controller

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/v1/transaction")
class TransactionsController(@Autowired private val transactionsRepository: TransactionsRepository) {

    @GetMapping
    public fun get(): List<Transaction> = transactionsRepository.findAll()

    @PostMapping
    fun create(@RequestBody transaction: Transaction): Transaction {
        return transactionsRepository.save(transaction.copy(uuid = UUID.randomUUID()))
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
        val uuid: UUID?,
        val accountNumber: BigDecimal,
        @Enumerated(EnumType.ORDINAL)
        val type: Type,
        val amount: Long,
        @CreationTimestamp
        val timestamp: LocalDateTime?
) {}

enum class Type {
    DEPOSIT, WITHDRAWAL
}

interface TransactionsRepository : JpaRepository<Transaction, UUID> {
    fun findAllByAccountNumber(accountNumber: BigDecimal): List<Transaction>
}