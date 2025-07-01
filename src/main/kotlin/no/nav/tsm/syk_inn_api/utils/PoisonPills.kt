package no.nav.tsm.syk_inn_api.utils

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class PoisonPills(
    @param:Value($$"${nais.cluster}") private val cluster: String,
) {
    val logger = logger()

    val devPoisonPills =
        setOf(
            "2db4d27e-6be8-4ed1-bc61-f167d58c8239",
            "929426f4-5e18-4a40-b01d-9bee3907e7f3",
            "e967c4c1-afa5-4968-8ac3-71f2a90c8353",
            "dc7e7969-6688-447b-9dc1-d18119e1038a",
            "b85180e3-24ca-45eb-b65c-8e7f8a3f408c",
            "0c16d665-6273-4447-b457-a9529bb63f2b",
            "f0d5fb38-3750-44ba-b3dd-8607d082d50b",
            "73d1619d-22e9-43ad-81bf-5b4904a55639",
            "0d890dc1-1ecf-4dd8-a353-177e3b7f0e7f",
            "4f034c28-5e98-41e2-a7c2-7d14f38a8e5b",
            "d41c1c9a-067c-4f10-9658-a9605fc6e8ed",
            "5b504585-de8f-45c6-a662-42124b9752c7",
            "099b0a70-d07a-4e6f-8ca5-23a6f7a9707a",
            "83163fa1-cc3d-4634-9320-3fec65df1d1b",
            "d807fd1a-b037-431b-a8c1-7c871197bb28",
            "9d1c49a5-91a6-44c2-9d46-dd9c3f183098",
            "a0e92440-2b88-40f1-ac69-70365a1137db",
            "f2fb61f3-d98f-467e-84ca-ceb942ffc5f0",
            "b2b69ba7-6b55-4593-bbcf-c63f857a6c7a",
            "1b2da8b6-02c4-4216-8d20-00ca2a31a009",
            "f55acf91-5d3d-405f-8734-83d4a3a9e419",
            "d074486e-fc06-43af-8212-9d7d1bb2af07",
            "68895bc9-b071-484e-81d0-3e2f0a74850a",
            "4293baa9-bfee-4861-8ed1-752ec7ec3d52",
            "8e021794-b84e-448f-831f-eb616815a20a",
            "42b5d3af-6487-47f3-a01d-afe26b9f1ecc",
            "e559f677-9b31-4d42-bbd5-45c705eba40f",
            "8a0e2777-c071-458c-ad24-c4a721659a3b",
            "4ae81d14-bb9f-4e57-a64d-d0f70d3ec0ab",
            "c877c802-5907-476d-b50d-079c6b6992b4",
            "55260250-07f6-4e4f-a23b-e0234cfdd2cf",
            "24b16b92-9552-4566-8668-b1aa3400b7e5",
            "a1f88393-8440-42ef-aa1f-cbd76bdf0930",
            "673addde-f9ec-47b2-be49-df805059d159",
            "8181e6e7-9ceb-4ef8-946f-7601bf114c0b",
            "aa625f45-3491-49d9-9579-7d473d3dccde",
            "2c758ed5-8018-47b0-9e8b-21a6a998ceb6",
            "c0be1665-b07b-4279-ada2-0f1f7ed6d738",
            "2ef53eae-cdac-4b40-a5b2-fe866e00ce3b",
            "d0b109e2-52da-443c-b4d4-9d722cf318d5",
            "696d1dd1-2b67-40e6-98f9-fbcb56201c02",
            "61118435-587b-4aee-a173-d951818787f2",
            "dbe01fca-871c-4c06-a492-f3cd5bd83a32",
            "43ec5742-e961-4a59-b8d5-33d09e349149",
            "480131a0-87da-4dde-bab9-2753b2d8026e",
            "59197563-4486-4695-874e-e5bf6b756d96",
            "7e4deb04-256c-47af-a4b3-469dbb610ea4",
            "0ac286a1-2141-4154-85dd-a941768ff245",
            "3705b862-cecc-4aa5-b98a-a3259a33a010",
        )

    fun isPoisoned(sykmeldingId: String): Boolean {
        return when (cluster) {
            "dev-gcp" -> devPoisonPills.contains(sykmeldingId)
            "prod-gcp" -> false
            "local" -> false
            else -> {
                logger.error(
                    "Unknown cluster: $cluster, cannot determine if sykmeldingId is poisoned",
                )
                false
            }
        }
    }
}
