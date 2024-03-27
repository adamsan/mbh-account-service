package hu.mbhbank.accountservice

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Parameter
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.id.enhanced.SequenceStyleGenerator
import org.hibernate.service.ServiceRegistry
import org.hibernate.type.Type
import org.hibernate.type.spi.TypeConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.web.bind.annotation.*
import java.util.*

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
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accounts_seq")
        @GenericGenerator(
                name = "accounts_seq",
                strategy = "hu.mbhbank.accountservice.CustomIdGenerator",
                parameters = [Parameter(name = "increment_size", value = "1")]
        )
        val accountNumber: Long?,
        val accountHolderName: String,
        val isDeleted: Boolean = false) {}

interface AccountsRepository : JpaRepository<Account, Long> {}

data class NewAccountDTO(val accountHolderName: String)

class CustomIdGenerator(): SequenceStyleGenerator() {
    val prefix = "1234000"
    override fun generate(session: SharedSessionContractImplementor?, `object`: Any?): Any {
        return (prefix + super.generate(session, `object`)).toLong()
    }

    override fun configure(type: Type?, parameters: Properties?, serviceRegistry: ServiceRegistry?) {
        super.configure(TypeConfiguration().basicTypeRegistry.getRegisteredType(Long::class.java), parameters, serviceRegistry)
        parameters?.setProperty(SEQUENCE_PARAM, "accounts_seq")
        parameters?.setProperty(INCREMENT_PARAM, "1")
    }
}