package hu.mbhbank.accountservice.transactions.controller

import hu.mbhbank.accountservice.accounts.dao.AccountsRepository
import hu.mbhbank.accountservice.screening.service.ScreeningService
import hu.mbhbank.accountservice.transactions.model.Transaction
import hu.mbhbank.accountservice.transactions.model.TransactionsRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@RestController
@RequestMapping("/api/v1/transaction")
class TransactionsController(
        private val transactionsRepository: TransactionsRepository,
        private val screeningService: ScreeningService,
        private val accountRepository: AccountsRepository
) {

    @GetMapping
    fun get(): List<Transaction> = transactionsRepository.findAll()

    @PostMapping
    fun create(@RequestBody transaction: Transaction): ResponseEntity<Transaction> {
        if (!screeningService.didAccountPassSecurityCheck(transaction.accountNumber)) {
            return ResponseEntity.notFound().build()
        }
        if (accountRepository.findByIsDeletedIsFalseAndAccountNumberEquals(transaction.accountNumber).isEmpty) {
            return ResponseEntity.notFound().build()
        }
        return if (isTransactionInThePast(transaction))
            ResponseEntity.badRequest().build()
        else
            ResponseEntity.ok(transactionsRepository.save(transaction.copy(uuid = UUID.randomUUID())))
    }

    // Transaction can't be in the past - let's say 500 msec delta is acceptable:
    // When transaction timestamp is not submitted, then it's generated as a default value in the constructor
    // of `Transaction`.
    // It takes time, until the program execution reaches here.
    private fun isTransactionInThePast(transaction: Transaction) =
            transaction.timestamp!!.isBefore(LocalDateTime.now().minus(500, ChronoUnit.MILLIS))

    @GetMapping("/{uuid}")
    fun getById(@PathVariable("uuid") uuid: UUID): Transaction {
        return transactionsRepository.findById(uuid).orElseThrow { NoSuchElementException("Transaction not found") }
    }

    @PutMapping("/{uuid}")
    fun update(@PathVariable("uuid") uuid: UUID, @RequestBody updatedTransaction: Transaction): Transaction {
        val existingTransaction = transactionsRepository.findById(uuid).orElseThrow { NoSuchElementException("Transaction not found") }
        if(isTransactionInThePast(updatedTransaction)) throw TransactionInPastException()
        val mergedTransaction = existingTransaction.copy(
                accountNumber = updatedTransaction.accountNumber,
                type = updatedTransaction.type,
                amount = updatedTransaction.amount,
                timestamp = updatedTransaction.timestamp
        )
        return transactionsRepository.save(mergedTransaction)
    }
}

class TransactionInPastException : RuntimeException()
