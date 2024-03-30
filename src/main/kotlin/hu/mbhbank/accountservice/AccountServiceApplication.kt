package hu.mbhbank.accountservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Bean
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@SpringBootApplication
@EnableFeignClients
class AccountServiceApplication {

    @Bean
    fun executorService(): ExecutorService {
        // since the tasks (sending a POST request) are not CPU heavy, I try to double the CPU size for threadpool size
        val threadPoolSize = 2 * Runtime.getRuntime().availableProcessors()
        return Executors.newFixedThreadPool(threadPoolSize)
    }
}

fun main(args: Array<String>) {
    runApplication<AccountServiceApplication>(*args)
}
