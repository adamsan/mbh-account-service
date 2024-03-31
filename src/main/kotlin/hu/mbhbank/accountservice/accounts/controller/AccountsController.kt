package hu.mbhbank.accountservice.accounts.controller

import hu.mbhbank.accountservice.accounts.dao.AccountsRepository
import hu.mbhbank.accountservice.accounts.model.Account
import hu.mbhbank.accountservice.screening.ScreeningService
import hu.mbhbank.accountservice.transactions.controller.TransactionsRepository
import hu.mbhbank.accountservice.transactions.controller.Type
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/v1/account")
class AccountsController(
        private val accountsRepository: AccountsRepository,
        private val transactionsRepository: TransactionsRepository,
        private val screeningService: ScreeningService
) {

    @GetMapping
    fun get(): List<Account> = accountsRepository.findByIsDeletedIsFalse()

    @GetMapping("/{id}")
    fun getById(@PathVariable("id") id: BigDecimal): ResponseEntity<Account> {
        val maybeAccount = accountsRepository.findByIsDeletedIsFalseAndAccountNumberEquals(id)
        return ResponseEntity.of(maybeAccount)
    }

    @GetMapping("/{id}/balance")
    fun getBalanceById(@PathVariable("id") id: BigDecimal): ResponseEntity<Long> {
        val maybeAccount = accountsRepository.findByIsDeletedIsFalseAndAccountNumberEquals(id)
        if (maybeAccount.isEmpty) return ResponseEntity.notFound().build()

        val transactions = transactionsRepository.findAllByAccountNumber(id)
        val balance = transactions.sumOf {
            when (it.type) {
                Type.DEPOSIT -> it.amount
                Type.WITHDRAWAL -> -it.amount
            }
        }
        return ResponseEntity.ok(balance)
    }

    @PostMapping
    fun post(@RequestBody accountDTO: AccountDTO): Account {
        val account = Account(null, accountDTO.accountHolderName)
        val savedAccount = accountsRepository.save(account)
        screeningService.requestScreening(account)
        return savedAccount
    }

    @DeleteMapping("/{id}")
    fun del(@PathVariable("id") id: BigDecimal): ResponseEntity<Account> {
        return if (accountsRepository.existsById(id)) {
            accountsRepository.findById(id).map {
                ResponseEntity.ok(accountsRepository.save(it.copy(isDeleted = true)))
            }.orElseThrow()
        } else
            ResponseEntity.notFound().build()
    }
}

data class AccountDTO(val accountHolderName: String)

