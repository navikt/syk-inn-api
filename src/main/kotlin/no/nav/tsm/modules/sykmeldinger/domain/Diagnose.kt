package no.nav.tsm.modules.sykmeldinger.domain

import no.nav.tsm.diagnoser.ICD10
import no.nav.tsm.diagnoser.ICPC2
import no.nav.tsm.diagnoser.ICPC2B
import no.nav.tsm.modules.behandler.payloads.SykInnDiagnoseSystem

fun SykInnDiagnoseSystem.getDiagnoseText(code: String): String =
    when (this) {
        SykInnDiagnoseSystem.ICD10 -> ICD10[code]?.text
        SykInnDiagnoseSystem.ICPC2 -> ICPC2[code]?.text
        SykInnDiagnoseSystem.ICPC2B -> ICPC2B[code]?.text
    } ?: throw IllegalStateException("Finner ikke diagnose for system $this og code $code")
