package no.nav.tsm.sykinnapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity

@SpringBootApplication @EnableWebSecurity class SykInnApiApplication

fun main(args: Array<String>) {
    runApplication<SykInnApiApplication>(*args)
}
