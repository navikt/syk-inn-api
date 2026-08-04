package no.nav.tsm.utils

import io.mockk.mockk
import no.nav.tsm.core.Environment
import no.nav.tsm.core.ExternalApi
import no.nav.tsm.core.Runtime
import no.nav.tsm.ktor.nais.RuntimeCluster

val simpleUnitTestEnvironment =
    Environment(
        runtime = Runtime(env = RuntimeCluster.PROD, name = "test-app", version = "testy-v0"),
        kafka = mockk(relaxed = true),
        jobs = mockk(relaxed = true),
        postgres = mockk(relaxed = true),
        sykmeldingConfig = mockk(relaxed = true),
        external = {
            ExternalApi(
                btsys = "https://test.btsys.endpoint",
                tsmPdlCache = "https://test.pdlcache.endpoint",
                helsenettproxy = "https://test.helsenettproxy.endpoint",
            )
        },
    )
