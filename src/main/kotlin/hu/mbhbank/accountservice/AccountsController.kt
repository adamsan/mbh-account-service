package hu.mbhbank.accountservice

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/account")
class AccountsController(@Autowired private val accountsRepository: AccountsRepository) {
    @GetMapping
    fun get(): List<Account> = accountsRepository.findAll()

    @PostMapping
    fun post(@RequestBody newAccountDTO: NewAccountDTO): Account {
        // TODO: generate unique id with bank prefix
        val account = Account(null, newAccountDTO.accountHolderName)
        accountsRepository.save(account)
        return account
    }
}

@Entity(name = "accounts")
data class Account(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_number_generator")
        @SequenceGenerator(name="account_number_generator", sequenceName = "accounts_seq", allocationSize = 1)
        val accountNumber: Long?,
        val accountHolderName: String,
        val isDeleted: Boolean = false) {}

interface AccountsRepository : JpaRepository<Account, Long> {}

data class NewAccountDTO(val accountHolderName: String)
