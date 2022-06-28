package operation.volume

import operation.AuthedHttpOperation
import operation.request.Requester
import utils.VolumeInfo


@kotlinx.serialization.Serializable
data class ListVolumeReq(
   @kotlinx.serialization.Transient val clusterId: String = ""
)

@kotlinx.serialization.Serializable
data class ListVolumeResp(
   val data: List<VolumeInfo>
)

class ListVolumeOperation: AuthedHttpOperation<ListVolumeReq, ListVolumeResp>(
    method = Requester.Method.GET,
    path = "/v1/volumes"
) {
   override suspend fun invoke(input: ListVolumeReq): ListVolumeResp {
      return getRequester().apply {
         addPathParameter(input.clusterId)
      }.send(input).parse()
   }
}