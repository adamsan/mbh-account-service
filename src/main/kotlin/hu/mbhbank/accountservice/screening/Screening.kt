package hu.mbhbank.accountservice.screening

import hu.mbhbank.accountservice.accounts.model.Account
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.servlet.ServletContext
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent
import org.springframework.cloud.openfeign.FeignClient
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
class ScreeningController(
        @Autowired val screeningService: ScreeningService
) {
    val logger: Logger = LoggerFactory.getLogger(ScreeningController::class.java)

    @PostMapping("/background-security-callback/{uuid}")
    fun receiveSecurityResult(@PathVariable("uuid") uuid: UUID, @RequestBody securityResponse: SecurityResponse) {
        logger.info("$uuid received")
        logger.info("$securityResponse received")
        // check if uuid in request table, and accountNumbers match, then save response
        screeningService.verifyAndStore(uuid, securityResponse)
    }
}

@FeignClient(name = "security-caller", url = "\${bank.background-security-check.url}")
interface SecurityCaller {

    @PostMapping
    fun call(securityRequestDTO: SecurityRequestDTO)
}

@Service
class ScreeningService(
        @Autowired val securityRequestRepository: SecurityRequestRepository,
        @Autowired val securityResponseRepository: SecurityResponseRepository,
        @Autowired val securityCaller: SecurityCaller
) {

    val logger: Logger = LoggerFactory.getLogger(ScreeningService::class.java)

    @Autowired
    lateinit var myUrlProvider: MyUrlProvider

    @Transactional
    fun requestScreening(acc: Account) {
        val securityRequest = SecurityRequest(acc.accountNumber!!, acc.accountHolderName)
        val callBackUrl = "${myUrlProvider.getMyUrl()}/api/v1/background-security-callback/${securityRequest.callbackUUID.toString()}"
        logger.info("callback url: $callBackUrl")
        val securityRequestDTO = SecurityRequestDTO(acc.accountNumber, acc.accountHolderName, callBackUrl)
        // send securityRequestDTO to securityUrl - in a new thread
        logger.info("Calling background-security-check with: $securityRequestDTO")
        securityCaller.call(securityRequestDTO)
        logger.info("Persisting background security check request")
        securityRequestRepository.save(securityRequest)
    }

    fun verifyAndStore(uuid: UUID, securityResponse: SecurityResponse) {
        val maybeSecurityRequest = securityRequestRepository.findById(uuid)
        if (maybeSecurityRequest.isEmpty) {
            logger.warn("Invalid verification attempt: no security request found for uuid:$uuid")
            return
        }
        if (maybeSecurityRequest.get().accountNumber != securityResponse.accountNumber) {
            logger.warn("Invalid verification attempt: account numbers don't match for uuid:$uuid")
            return
        }
        logger.info("Persisting security response: $securityResponse")
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
class MyUrlProvider(private val servletContext: ServletContext) : ApplicationListener<ServletWebServerInitializedEvent> {

    private var serverPort: Int = 0

    // add localhost as default value instead of lateinit, so tests won't fail
    private var serverAddress: InetAddress = InetAddress.getLocalHost()

    override fun onApplicationEvent(event: ServletWebServerInitializedEvent) {
        serverPort = event.webServer.port
        serverAddress = InetAddress.getLocalHost()
    }

    fun getMyUrl(): String {
        val contextPath = servletContext.contextPath
        val contextPathPrefix = if (contextPath.isNotBlank() && contextPath != "/") "$contextPath/" else ""
        return "http://${serverAddress.hostAddress}:$serverPort$contextPathPrefix".removeSuffix("/")
    }
}
