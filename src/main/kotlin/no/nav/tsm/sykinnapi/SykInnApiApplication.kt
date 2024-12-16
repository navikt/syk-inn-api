package no.nav.tsm.sykinnapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication class SykInnApiApplication

fun main(args: Array<String>) {
    runApplication<SykInnApiApplication>(*args)
}
