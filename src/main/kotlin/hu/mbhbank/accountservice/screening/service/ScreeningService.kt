package hu.mbhbank.accountservice.screening.service

import hu.mbhbank.accountservice.accounts.model.Account
import hu.mbhbank.accountservice.screening.model.SecurityRequest
import hu.mbhbank.accountservice.screening.model.SecurityRequestRepository
import hu.mbhbank.accountservice.screening.model.SecurityResponse
import hu.mbhbank.accountservice.screening.model.SecurityResponseRepository
import jakarta.servlet.ServletContext
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PostMapping
import java.math.BigDecimal
import java.net.InetAddress
import java.util.*
import java.util.concurrent.ExecutorService
import kotlin.jvm.optionals.getOrElse

@FeignClient(name = "security-caller", url = "\${bank.background-security-check.url}")
interface SecurityCaller {

    @PostMapping
    fun call(securityRequestDTO: SecurityRequestDTO)
}

@Service
class ScreeningService(
        val securityRequestRepository: SecurityRequestRepository,
        val securityResponseRepository: SecurityResponseRepository,
        val securityCaller: SecurityCaller,
        val executorService: ExecutorService,
        val myUrlProvider: MyUrlProvider
) {
    val logger: Logger = LoggerFactory.getLogger(ScreeningService::class.java)

    @Transactional
    fun requestScreening(acc: Account) {
        val securityRequest = SecurityRequest(acc.accountNumber!!, acc.accountHolderName)
        val callBackUrl = "${myUrlProvider.getMyUrl()}/api/v1/background-security-callback/${securityRequest.callbackUUID.toString()}"
        logger.info("callback url: $callBackUrl")
        val securityRequestDTO = SecurityRequestDTO(acc.accountNumber, acc.accountHolderName, callBackUrl)

        logger.info("Persisting background security check request")
        securityRequestRepository.save(securityRequest)
        securityRequestRepository.flush()

        // send securityRequestDTO to securityUrl - in a new thread - possibly would be better with Async?
        logger.info("Calling background-security-check with: $securityRequestDTO")
        executorService.submit { securityCaller.call(securityRequestDTO) }
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