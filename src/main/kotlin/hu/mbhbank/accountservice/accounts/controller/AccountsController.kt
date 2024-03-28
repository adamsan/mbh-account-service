package hu.mbhbank.accountservice.accounts.controller

import hu.mbhbank.accountservice.accounts.dao.AccountsRepository
import hu.mbhbank.accountservice.accounts.model.Account
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/v1/account")
class AccountsController(@Autowired private val accountsRepository: AccountsRepository) {

    @GetMapping
    public fun get(): List<Account> = accountsRepository.findByIsDeletedIsFalse()

    @GetMapping("/{id}")
    public fun getById(@PathVariable("id") id: BigDecimal): ResponseEntity<Account> {
        val maybeAccount = accountsRepository.findByIsDeletedIsFalseAndAccountNumberEquals(id)
        return ResponseEntity.of(maybeAccount)
    }

    @PutMapping("/{id}")
    public fun update(@PathVariable("id") id: BigDecimal, @RequestBody dto: AccountDTO): ResponseEntity<Account> {
        val maybeAccount = accountsRepository.findByIsDeletedIsFalseAndAccountNumberEquals(id)
        if(maybeAccount.isEmpty) return ResponseEntity.notFound().build()
        val updatedAccount = accountsRepository.save(maybeAccount.get().copy(accountHolderName = dto.accountHolderName))
        return ResponseEntity.ok(updatedAccount)
    }

    @PostMapping
    public fun post(@RequestBody accountDTO: AccountDTO): Account {
        val account = Account(null, accountDTO.accountHolderName)
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

data class AccountDTO(val accountHolderName: String)
