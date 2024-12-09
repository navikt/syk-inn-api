package no.nav.tsm.sykinnapi

import no.nav.tsm.sykinnapi.util.Cluster.Companion.profiler
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity

@SpringBootApplication @EnableWebSecurity class SykInnApiApplication

fun main(args: Array<String>) {
    runApplication<SykInnApiApplication>(*args) {
        setAdditionalProfiles(*profiler) // sett  profilerer automatisk
    }
}
