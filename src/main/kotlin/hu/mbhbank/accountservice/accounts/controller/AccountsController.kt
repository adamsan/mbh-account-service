package hu.mbhbank.accountservice.accounts.controller

import hu.mbhbank.accountservice.accounts.dao.AccountsRepository
import hu.mbhbank.accountservice.accounts.model.Account
import hu.mbhbank.accountservice.screening.service.ScreeningService
import hu.mbhbank.accountservice.transactions.model.TransactionsRepository
import hu.mbhbank.accountservice.transactions.model.Type
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@Tag(name = "accounts-controller", description = "handles account requests")
@RestController
@RequestMapping("/api/v1/account")
class AccountsController(
    private val accountsRepository: AccountsRepository,
    private val transactionsRepository: TransactionsRepository,
    private val screeningService: ScreeningService
) {

    @Operation(
        summary = "Get accounts",
        description = "Returns with all (non deleted) accounts",
    )
    @ApiResponse(
        responseCode = "200", description = "List of non deleted accounts.",
        content = [Content(
            mediaType = "application/json", schema = Schema(implementation = Account::class, type = "array")
        )]
    )
    @GetMapping
    fun get(): List<Account> = accountsRepository.findByIsDeletedIsFalse()

    @Operation(
        summary = "Get account by account_number (24 digit code)",
        description = "Returns a single account, which has an account_number (24 digit code) as requested",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Account found and returned",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Account::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Account not found for the given account_number",
                content = [Content()]
            )
        ]
    )
    @GetMapping("/{id}")
    fun getById(@PathVariable("id") id: BigDecimal): ResponseEntity<Account> {
        val maybeAccount = accountsRepository.findByIsDeletedIsFalseAndAccountNumberEquals(id)
        return ResponseEntity.of(maybeAccount)
    }

    @Operation(
        summary = "Get balance by account number",
        description = "Retrieves the balance of an account by the account number"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Account balance retrieved successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(type = "number", format = "int64")
                )]
            ),
            ApiResponse(
                responseCode = "404", description = "Account not found for the given account number",
                content = [Content()]
            )
        ]
    )
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

    @Operation(
        summary = "Create an account",
        description = "Creates a new account with the provided account information, and starts the background security check."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Account created successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Account::class)
                )]
            )
        ]
    )
    @PostMapping
    fun post(@RequestBody accountDTO: AccountDTO): Account {
        // TODO: Swagger doesn't display response properly, BigDecimal is displayed in scientific notation
        // solution could be to return a different type with the account number as a string
        val account = Account(null, accountDTO.accountHolderName)
        val savedAccount = accountsRepository.save(account)
        screeningService.requestScreening(account)
        return savedAccount
    }

    @Operation(
        summary = "Delete an account",
        description = "Deletes an account with the provided account number."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Account deleted successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = Account::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Account not found for the given account number",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema()
                )]
            )
        ]
    )
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

