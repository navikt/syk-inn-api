package no.nav.tsm.sykinnapi.modell.syfosmregister

import java.time.LocalDate

data class MedisinskVurderingDTO(
    val hovedDiagnose: DiagnoseDTO?,
    val biDiagnoser: List<DiagnoseDTO>,
    val annenFraversArsak: AnnenFraversArsakDTO?,
    val svangerskap: Boolean,
    val yrkesskade: Boolean,
    val yrkesskadeDato: LocalDate?,
)
