package io.viascom.github.action.greeter

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.time.LocalDateTime

@SpringBootApplication
open class GitHubActionApplication : CommandLineRunner {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<GitHubActionApplication>(*args)
        }
    }

    private val log = LoggerFactory.getLogger(GitHubActionApplication::class.java)

    override fun run(vararg args: String?) {
        var envWhoToGreet = System.getenv("INPUT_WHO_TO_GREET")
        if (envWhoToGreet == null) {
            envWhoToGreet = "anonymous :)"
        }

        log.info("Hello {}", envWhoToGreet)

        val now = LocalDateTime.now()
        log.info("::set-output name=time::$now")
    }
}