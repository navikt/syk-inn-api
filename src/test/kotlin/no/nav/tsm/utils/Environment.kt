package no.nav.tsm.utils

import io.mockk.mockk
import no.nav.tsm.core.Environment
import no.nav.tsm.core.ExternalApi
import no.nav.tsm.core.Runtime
import no.nav.tsm.core.RuntimeEnvironments
import no.nav.tsm.core.Texas

val simpleUnitTestEnvironment =
    Environment(
        runtime = Runtime(env = RuntimeEnvironments.PROD, name = "test-app", version = "testy-v0"),
        texas = { Texas(tokenEndpoint = "https://test.token.endpoint") },
        kafka = mockk(relaxed = true),
        jobs = mockk(relaxed = true),
        postgres = mockk(relaxed = true),
        sykmeldingConfig = mockk(relaxed = true),
        external = {
            ExternalApi(
                btsys = "https://test.btsys.endpoint",
                tsmPdlCache = "https://test.pdlcache.endpoint",
                helsenettproxy = "https://test.helsenettproxy.endpoint",
                hpr = "https://test.hpr.endpoint",
            )
        },
        auth = mockk(relaxed = true),
    )
