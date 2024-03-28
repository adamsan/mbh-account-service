package hu.mbhbank.accountservice.accounts.dao

import hu.mbhbank.accountservice.accounts.model.Account
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal
import java.util.*

interface AccountsRepository : JpaRepository<Account, BigDecimal> {
    fun findByIsDeletedIsFalse(): List<Account>
    fun findByIsDeletedIsFalseAndAccountNumberEquals(id: BigDecimal): Optional<Account>
}