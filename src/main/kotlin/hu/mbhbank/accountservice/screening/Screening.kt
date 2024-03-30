package hu.mbhbank.accountservice.screening

import hu.mbhbank.accountservice.accounts.model.Account
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent
import org.springframework.context.ApplicationListener
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.net.InetAddress
import java.util.*
import kotlin.jvm.optionals.getOrElse


@RestController
@RequestMapping("/api/v1")
class SceeningController(
        @Autowired val screeningService: ScreeningService
) {
    @PostMapping("/background-security-callback/{uuid}")
    fun receiveSecurityResult(@PathVariable("uuid") uuid: UUID, @RequestBody securityResponse: SecurityResponse) {
        println("$uuid received")
        println("$securityResponse received")
        // TODO: check if uuid in request table, and accountNumbers match, then save response
        screeningService.verifyAndStore(uuid, securityResponse)
    }
}

@Service
class ScreeningService(
        @Autowired val securityRequestRepository: SecurityRequestRepository,
        @Autowired val securityResponseRepository: SecurityResponseRepository
) {

    @Value("\$bank.background-security-check.url")
    lateinit var securityUrl: String

    @Autowired
    lateinit var myUrlProvider: MyUrlProvider

    fun requestScreening(acc: Account) {
        val securityRequest = SecurityRequest(acc.accountNumber!!, acc.accountHolderName)
        val callBackUrl = "${myUrlProvider.getMyUrl()}/api/v1/background-security-callback/${securityRequest.callbackUUID.toString()}"
        val securityRequestDTO = SecurityRequestDTO(acc.accountNumber, acc.accountHolderName, callBackUrl)
        // send securityRequestDTO to securityUrl - in a new thread
        securityRequestRepository.save(securityRequest)

    }

    fun verifyAndStore(uuid: UUID, securityResponse: SecurityResponse) {
        val maybeSecurityRequest = securityRequestRepository.findById(uuid)
        if (maybeSecurityRequest.isEmpty || maybeSecurityRequest.get().accountNumber != securityResponse.accountNumber) {
            // TODO: log failed verification attempt
            return
        }
        securityResponseRepository.save(securityResponse)
    }

    fun didAccountPassSecurityCheck(accountNumber: BigDecimal): Boolean =
            securityResponseRepository.findById(accountNumber).map { it.isSecurityCheckSuccess }.getOrElse { false }

}

data class SecurityRequestDTO(
        val accountNumber: BigDecimal,
        val accountHolderName: String,
        val callbackUrl: String
)

@Entity
data class SecurityRequest(
        val accountNumber: BigDecimal,
        val accountHolderName: String,
        @Id
        @Column(columnDefinition = "uuid") // https://stackoverflow.com/questions/70185764/unable-to-store-uuids-in-h2-2-0-202-with-hibernate/70205843#70205843
        val callbackUUID: UUID? = UUID.randomUUID()
)

interface SecurityRequestRepository : JpaRepository<SecurityRequest, UUID> {

}

@Entity
data class SecurityResponse(
        @Id
        val accountNumber: BigDecimal,
        val isSecurityCheckSuccess: Boolean
)

interface SecurityResponseRepository : JpaRepository<SecurityResponse, BigDecimal>

@Component
class MyUrlProvider : ApplicationListener<ServletWebServerInitializedEvent> {

    private var serverPort: Int = 0

    // add localhost as default value instead of lateinit, so tests won't fail
    private var serverAddress: InetAddress = InetAddress.getLocalHost()

    override fun onApplicationEvent(event: ServletWebServerInitializedEvent) {
        serverPort = event.webServer.port
        serverAddress = InetAddress.getLocalHost()
    }

    fun getMyUrl(): String {
        return "http://${serverAddress.hostAddress}:$serverPort"
    }
}
