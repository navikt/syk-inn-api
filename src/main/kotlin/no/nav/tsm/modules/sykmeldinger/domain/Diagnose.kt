package no.nav.tsm.modules.sykmeldinger.domain

import no.nav.tsm.core.common.SykInnDiagnoseSystem
import no.nav.tsm.diagnoser.ICD10
import no.nav.tsm.diagnoser.ICPC2
import no.nav.tsm.diagnoser.ICPC2B

fun SykInnDiagnoseInfo.text(): String =
    when (system) {
        SykInnDiagnoseSystem.ICD10 -> ICD10[code]?.text
        SykInnDiagnoseSystem.ICPC2 -> ICPC2[code]?.text
        SykInnDiagnoseSystem.ICPC2B -> ICPC2B[code]?.text
    } ?: throw IllegalStateException("Finner ikke diagnose for system $system og code $code")
