# HCDProject

### Intro
- This is a testing software developed for HCD.
- Some demo can be found under /demo/

### Usage
#### requirements: jdk1.8.0_131
- The static folder and its contents must be in the working directory
- All user data is under /data/ in the working directory, which can be relocated directly during migration
- build fat jar with gradle task: `gradlew shadowJar`
- use `java -jar {name_of_the_jar}.jar` to run

### Continue Development

#### Section I: User Data Management
- User data is stored in a local filesystem, each user has it own file, can be found under `/data/userdata/{user_id}.{serialization_format}`
- Each file can be deserialized into a polymorphic list, each data in the list is a class extends to `sealed class UserData`
- Data can be retrieved by `user.getAllData<TypeOfData>()`
- Data can be modified by using `DataScope`, example:
```kotlin
//open a data scope, it represents all data belongs to user with type 'ConnectionHistory'
user.dataScope<ConnectionHistory> { it ->
    //do normal list operation
    it.removeIf { ele -> ele.portal == data.portal }
    it.add(
        0, ConnectionHistory(
            data.portal, data.username, data.password
        )
    )
    //true means the data is modified, false means the data is not modified
    //if the data is not modified, the data will not be saved
    true
}
```
#### Section II: Add Restful API
- `Requester` is introduced as an encapsulated tool for HTTP communication. It uses a `data class` as input and a `data class` as server response. How the `data class` is formatted in the real HTTP request depends on `Requester.Method`

| Method    | How input is encoded                           |
|-----------|------------------------------------------------|
| GET       | `input data object` is encoded as query string |
| POST      | `input data object` is encoded as JSON         |
| PUT       | `input data object` is encoded as JSON         |
| DELETE    | `input data object` is encoded as query string |
| FORM_POSE | `input data object` is encoded as form data    |


- `Operation` is introduced as a stateless, immutable object describe the process of exchanging `input` to `output` `<I and O>`
- `HttpOperation` and `AuthedHttpOperation` are the actual class need for implementing new RESTful API
- `AuthedHttpOperation` automatically add authentication token to the request based on the portal that is specific
- Both `HttpOperation` and `AuthedHttpOperation` does not bind to any portal, it must be binded manually, such as
```kotlin
//register the key exchange service
//this is usually unnecessary in the real code
//when user log into portal, the key will be maintained
KeyExchangeService.register("172.16.4.248:8443")
//manually bind the portal
println(ListHostOperation().apply { 
    portal = "172.16.4.248:8443"
}(ListHostReq())) //this will print out ListHostResp
```
- Implementing a new http operation is very simple, just follow the following example:
```kotlin
//define the input structure
@kotlinx.serialization.Serializable
data class DiskAddTagReq(
    val hostId: String,
    val diskIds: List<String>,
    val diskTag: String
)

//define the output structure
@kotlinx.serialization.Serializable
class DiskAddTagResp(
    val taskId: String
)

//define the operation, Input type is DiskAddTagReq, Output type is DiskAddTagResp
class DiskAddTagOperation:AuthedHttpOperation<DiskAddTagReq,DiskAddTagResp>(
    method = Requester.Method.POST,//what method should the Requester use
    path = "/v1/disks/tag/auto-enable" //what's the path
){
    override suspend fun invoke(input: DiskAddTagReq): DiskAddTagResp = 
        //how to send the request
        getRequester().send(input).parse()
}
```
- It is recommended to write one operation in one file.
#### Section III: Add SSH operations

