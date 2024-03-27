package hu.mbhbank.accountservice

import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/account")
class AccountsController(
        @Autowired private val accountsRepository:AccountsRepository
) {
    @GetMapping
    fun get() = accountsRepository.findAll()
}

@Entity(name = "accounts")
data class Account(
        @Id val accountNumber: Long,
        val accountHolderName:String,
        val isDeleted: Boolean
) {
}

interface AccountsRepository : JpaRepository<Account, Long> {}