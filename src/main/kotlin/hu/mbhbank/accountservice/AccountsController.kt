package hu.mbhbank.accountservice

import jakarta.annotation.PostConstruct
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
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.*

@RestController
@RequestMapping("/api/v1/account")
class AccountsController(@Autowired private val accountsRepository: AccountsRepository) {

    @GetMapping
    public fun get(): List<Account> = accountsRepository.findByIsDeletedIsFalse()

    @GetMapping("/{id}")
    public fun getById(@PathVariable("id") id: BigDecimal): ResponseEntity<Account> {
        val maybeId = accountsRepository.findByIsDeletedIsFalseAndAccountNumberEquals(id)
        return ResponseEntity.of(maybeId)
    }

    @PostMapping
    public fun post(@RequestBody newAccountDTO: NewAccountDTO): Account {
        val account = Account(null, newAccountDTO.accountHolderName)
        accountsRepository.save(account)
        return account
    }

    @DeleteMapping("/{id}")
    public fun del(@PathVariable("id") id: BigDecimal): ResponseEntity<Account> {
        return if (accountsRepository.existsById(id)) {
            accountsRepository.findById(id).map {
                ResponseEntity.ok(accountsRepository.save(it.copy(isDeleted = true)))
            }.orElseThrow()
        } else
            ResponseEntity.notFound().build()
    }
}

private const val BANK_ACCOUNT_PREFIX = "bank.account.prefix"

@Component
class PrefixSetterConfig() {
    @Value("\${bank.account.prefix}")
    public lateinit var bankAccountPrefix: String

    @PostConstruct
    fun postConstruct() = System.setProperty(BANK_ACCOUNT_PREFIX, bankAccountPrefix)
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
        val accountNumber: BigDecimal?,
        val accountHolderName: String,
        val isDeleted: Boolean = false) {}

interface AccountsRepository : JpaRepository<Account, BigDecimal> {
    fun findByIsDeletedIsFalse(): List<Account>
    fun findByIsDeletedIsFalseAndAccountNumberEquals(id: BigDecimal): Optional<Account>
}

data class NewAccountDTO(val accountHolderName: String)

class CustomIdGenerator() : SequenceStyleGenerator() {
    private var prefix: String = "55555555"
    private val accountNumberLength = 24
    private var isPrefixSet = false
    override fun generate(session: SharedSessionContractImplementor?, `object`: Any?): Any {
        // during the running of the configure method, spring isn't finished with PrefixSetterConfig's PostConstruct
        // so reading the prefix at first generation of a key is a workaround against this
        if (!isPrefixSet) {
            prefix = System.getProperty(BANK_ACCOUNT_PREFIX, prefix)
            isPrefixSet = true
        }
        val maxId: String = super.generate(session, `object`).toString()
        val concatenated: String = prefix + maxId.padStart(accountNumberLength - prefix.length, '0')
        return concatenated.toBigDecimal()
    }

    override fun configure(type: Type?, parameters: Properties?, serviceRegistry: ServiceRegistry?) {
        super.configure(TypeConfiguration().basicTypeRegistry.getRegisteredType(BigDecimal::class.java), parameters, serviceRegistry)
        parameters?.setProperty(SEQUENCE_PARAM, "accounts_seq")
        parameters?.setProperty(INCREMENT_PARAM, "1")
    }
}