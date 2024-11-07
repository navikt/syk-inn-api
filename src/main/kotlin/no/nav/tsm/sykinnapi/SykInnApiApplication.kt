package no.nav.tsm.sykinnapi

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@EnableJwtTokenValidation @SpringBootApplication class SykInnApiApplication

fun main(args: Array<String>) {
    runApplication<SykInnApiApplication>(*args)
}
