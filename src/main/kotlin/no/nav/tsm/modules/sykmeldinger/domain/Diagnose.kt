package no.nav.tsm.modules.sykmeldinger.domain

import no.nav.tsm.core.common.SykInnDiagnoseSystem
import no.nav.tsm.diagnoser.ICD10
import no.nav.tsm.diagnoser.ICPC2
import no.nav.tsm.diagnoser.ICPC2B

fun SykInnDiagnoseSystem.maybeText(code: String): String? =
    when (this) {
        SykInnDiagnoseSystem.ICD10 -> ICD10[code]?.text
        SykInnDiagnoseSystem.ICPC2 -> ICPC2[code]?.text
        SykInnDiagnoseSystem.ICPC2B -> ICPC2B[code]?.text
    }
