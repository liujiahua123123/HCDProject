package operation.volumeaccessgroup

import operation.AuthedHttpOperation
import operation.request.Requester

data class AddVolumeAccessGroupVolumeReq(
    val volumeAccessGroupId: String,
    val clusterId: String,
    val volumeId: String
)

data class AddVolumeAccessGroupInitiatorReq(
    val volumeAccessGroupId: String,
    val clusterId: String,
    val initiatorId: String
)

data class RemoveVolumeAccessGroupInitiatorReq(
    val volumeAccessGroupId: String,
    val clusterId: String,
    val initiatorId: String,
)

data class RemoveVolumeAccessGroupVolumeReq(
    val volumeAccessGroupId: String,
    val clusterId: String,
    val volumeId: String,
)

@kotlinx.serialization.Serializable
class AddVolumeAccessGroupVolumeResp

@kotlinx.serialization.Serializable
class RemoveVolumeAccessGroupVolumeResp

@kotlinx.serialization.Serializable
class RemoveVolumeAccessGroupInitiatorResp

@kotlinx.serialization.Serializable
class AddVolumeAccessGroupInitiatorResp

class AddVolumeAccessGroupVolumeOperation: AuthedHttpOperation<AddVolumeAccessGroupVolumeReq,AddVolumeAccessGroupVolumeResp>(
    path = "/v1/volume-access-groups/",
    method = Requester.Method.POST
){
    override suspend fun invoke(input: AddVolumeAccessGroupVolumeReq): AddVolumeAccessGroupVolumeResp = this.getRequester()
        .apply {
            addPathParameter(input.clusterId)
            addPathParameter(input.volumeAccessGroupId)
            addPathParameter("add-volumes")
        }.send(listOf(input.volumeId)).parse()
}

class AddVolumeAccessGroupInitiatorOperation: AuthedHttpOperation<AddVolumeAccessGroupInitiatorReq,AddVolumeAccessGroupInitiatorResp>(
    path = "/v1/volume-access-groups/",
    method = Requester.Method.POST
){
    override suspend fun invoke(input: AddVolumeAccessGroupInitiatorReq): AddVolumeAccessGroupInitiatorResp = this.getRequester()
        .apply {
            addPathParameter(input.clusterId)
            addPathParameter(input.volumeAccessGroupId)
            addPathParameter("add-initiators")
        }.send(listOf(input.initiatorId)).parse()
}


class RemoveVolumeAccessGroupInitiatorOperation: AuthedHttpOperation<RemoveVolumeAccessGroupInitiatorReq,RemoveVolumeAccessGroupInitiatorResp>(
    path = "/v1/volume-access-groups/",
    method = Requester.Method.POST
){
    override suspend fun invoke(input: RemoveVolumeAccessGroupInitiatorReq): RemoveVolumeAccessGroupInitiatorResp = this.getRequester()
        .apply {
            addPathParameter(input.clusterId)
            addPathParameter(input.volumeAccessGroupId)
            addPathParameter("remove-initiators")
        }.send(listOf(input.initiatorId)).parse()
}

class RemoveVolumeAccessGroupVolumeOperation: AuthedHttpOperation<RemoveVolumeAccessGroupVolumeReq,RemoveVolumeAccessGroupVolumeResp>(
    path = "/v1/volume-access-groups/",
    method = Requester.Method.POST
){
    override suspend fun invoke(input: RemoveVolumeAccessGroupVolumeReq): RemoveVolumeAccessGroupVolumeResp = this.getRequester()
        .apply {
            addPathParameter(input.clusterId)
            addPathParameter(input.volumeAccessGroupId)
            addPathParameter("remove-volumes")
        }.send(listOf(input.volumeId)).parse()
}
