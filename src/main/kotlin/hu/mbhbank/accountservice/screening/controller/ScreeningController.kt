package hu.mbhbank.accountservice.screening.controller

import hu.mbhbank.accountservice.screening.service.ScreeningService
import hu.mbhbank.accountservice.screening.model.SecurityResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1")
class ScreeningController(
        val screeningService: ScreeningService
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