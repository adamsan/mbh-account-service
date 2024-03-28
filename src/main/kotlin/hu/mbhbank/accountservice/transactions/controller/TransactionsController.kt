package hu.mbhbank.accountservice.transactions.controller

import jakarta.persistence.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/v1/transaction")
class TransactionsController(@Autowired private val transactionsRepository: TransactionsRepository) {

    @GetMapping
    public fun get(): List<Transaction> = transactionsRepository.findAll()
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
        @Column(name = "timestamp", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
        val timestamp: LocalDateTime
) {}

enum class Type {
    DEPOSIT, WITHDRAWAL
}

interface TransactionsRepository : JpaRepository<Transaction, UUID> {

}