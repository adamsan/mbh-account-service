package hu.mbhbank.accountservice.transactions.model

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

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
)

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