package no.nav.tsm.syk_inn_api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication @EnableScheduling class SykInnApiApplication

fun main(args: Array<String>) {
    runApplication<SykInnApiApplication>(*args)
}
