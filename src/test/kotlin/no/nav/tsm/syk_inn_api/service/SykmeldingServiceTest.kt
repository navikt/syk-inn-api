package no.nav.tsm.syk_inn_api.service

class SykmeldingServiceTest {
}


//What a useful test here would look like:
//In your unit test for SykmeldingService, you would:
//
//Mock HelsenettProxyService, RuleService, and SykmeldingRepository (once real).
//
//Call createSykmelding(...) with a mock payload.
//
//Assert that:
//
//The result is ResponseEntity.status(CREATED)... when everything succeeds.
//
//You get BAD_REQUEST if ruleService.validateRules(...) fails.
//
//You get INTERNAL_SERVER_ERROR if the repository stub returns null or the Kafka step fails.
//
//Optionally verify interactions (e.g. verify(exactly = 1) { helsenettProxyService.getSykmelderByHpr(...) }).
//
