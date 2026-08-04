package no.nav.tsm.modules.sykmeldinger.jobs.juridisk

import no.nav.tsm.regulus.regula.juridisk.JuridiskVurdering

/** The actual record to be published on Kafka. Don't rename any properties here. :-) */
data class JuridiskHenvisningRecord(val juridiskeVurderinger: List<JuridiskVurdering>)
