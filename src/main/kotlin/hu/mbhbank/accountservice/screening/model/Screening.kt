package hu.mbhbank.accountservice.screening.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal
import java.util.*

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
