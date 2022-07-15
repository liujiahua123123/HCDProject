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

## Continue Development

### Section I: User Data Management

- User data is stored in a local filesystem, each user has it own file, can be found
  under `/data/userdata/{user_id}.{serialization_format}`
- Each file can be deserialized into a polymorphic list, each data in the list is a class extends
  to `sealed class UserData`
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

### Section II: Add Restful API

- `Requester` is introduced as an encapsulated tool for HTTP communication. It uses a `data class` as input and
  a `data class` as server response. How the `data class` is formatted in the real HTTP request depends
  on `Requester.Method`

| Method    | How input is encoded                           |
|-----------|------------------------------------------------|
| GET       | `input data object` is encoded as query string |
| POST      | `input data object` is encoded as JSON         |
| PUT       | `input data object` is encoded as JSON         |
| DELETE    | `input data object` is encoded as query string |
| FORM_POSE | `input data object` is encoded as form data    |

- `Operation` is introduced as a stateless, immutable object describe the process of exchanging `input`
  to `output` `<I and O>`
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

- In Section V: New Backend Feature, we will introduce a new backend feature that automatically bind portal to the
  operation
- Implementing a new http operation is very simple, just follow the following example:

```kotlin
//define the input structure
//MUST HAVE @kotlinx.serialization.Serializable
@kotlinx.serialization.Serializable
data class DiskAddTagReq(
    val hostId: String,
    val diskIds: List<String>,
    val diskTag: String
)

//define the output structure
//MUST HAVE @kotlinx.serialization.Serializable
@kotlinx.serialization.Serializable
class DiskAddTagResp(
    val taskId: String
)

//define the operation, Input type is DiskAddTagReq, Output type is DiskAddTagResp
class DiskAddTagOperation : AuthedHttpOperation<DiskAddTagReq, DiskAddTagResp>(
    method = Requester.Method.POST,//what method should the Requester use
    path = "/v1/disks/tag/auto-enable" //what's the path
) {
    override suspend fun invoke(input: DiskAddTagReq): DiskAddTagResp =
        //how to send the request
        getRequester().send(input).parse()
}
```

- It is recommended to write one operation in one file.

### Section III: Add SSH operations

- `HCDSshClient` is a wrapper for `SshClient`, example:

```kotlin 
 val client = HCDSshClient(name = {name shows on logger}, address = {ip_address}, user={linux user}, password={linux password})
 
 //execute a command, 
 client.execute("cd /home/{user}/ && ls -l")
```

- `HCDSshClient` does not support reading a response, you can edit the logger stream inside `HCDClient` to achieve this
  feature

### Section IV: New Front-end Feature

- Front-end use `vue` and `mdui`, `mdui` is a css library, can be found here https://www.mdui.org/
- The only method supports communicate with backend (an ajax wrapper) is `document.datapost`

```js 
$("#create-cluster").click(function () {
    //first parameter: path,
    //second parameter: data,  
    //third parameter: callback function(success, data), must have!
    document.dataPost("/cluster/new", {
        cluster: {
            clusterName: $("#create-cluster-cluster-name").val(),
            minClusterSize: $("#create-cluster-min-cluster-size").val(),
            replicationFactor: $("#create-cluster-replication-factor").val(),
            virtualIp: $("#create-cluster-virtual-ip").val(),
        },
        hosts: all_selected_host()
    }, function (_b, _a) {
        if (_b) {
            console.log("Create cluster success")
        } else {
            console.log("Create cluster failed")
        }
        console.log("Server response to json: ")
        console.log(_a)
    })
})
```

- this function will handle shadow layer, the percentage bar, and potential error response.
- you only need to care about what to send and what to do after receiving the response
- if callback function throws an error, it will be considered as a 'unable to parse server response'
- source code is under `/static/utils.js`

### Section V: New Backend Feature

- The request server send will be dispatch to correspond route, routes are under /server/route/
- Use any file under those to add new route will be fine, there are no restrictions, just to to any of them and add:

```kotlin 
handleDataPost("/cluster/new") {

}
```

- `handleDataPost` is the corresponding function to handle `document.dataPost`

##### Reading request

- use `call.readDataRequest<T>()` to read the front-end request, T class be will returned if the request is valid,
  otherwise it will quit the code block and send error message to front-end
- T must be marked as `@kotlinx.serialization.Serializable`. Any nested data class will also
  require `@kotlinx.serialization.Serializable`

##### Sending response - General

- use  `ApplicationCall.respondOK` or `ApplicationCall.respondTraceable` or `ApplicationCall.respondThrowable` to send
  response
- use `userInputError` to raise an error and quit the code block, the front-end will receive the error message without
  stacktrace
- any other throws in the code block will be caught and sent to front-end along with the stacktrace

#### Sending response - With Progress Bar

- For request that take time (maybe two or more Operation involved), it is recommended to show a progress bar to the
  front-end
- ApplicationCall.respondTraceable will switch the task to backend and let the front-end automatically trace the
  progress.
- The idea of `Traceable` is similar to `Future` in Java
- One implementation of Traceable is `OperationExecutor.addExecutorTask()`
- Code in the `OperationExecutor.addExecutorTask<ReturnType>{}` will be run in another thread-pool and will directly
  return a `Traceable`.` reponseTraceable` will handle the rest of the work

#### Sending response - Other Tools

- There are some tools that can be used inside this code block.
- `ifLogin{}` will check if the user is logged in, if not, it will redirect to login page
- `ifFromPortalPage{}` will check if the user is from the portal page && login, if not, it will redirect to portal page
- `httpOperationScope(portal){}` bind HttpOperations inside the code block to the portal automatically

#### Sending response - Example 1

```kotlin

@kotlinx.serialization.Serializable
//how the request structure is defined
data class CreateClusterReq(
    val cluster: CreateClusterInfo,
    val hosts: List<String>
)

//register the route '/cluster/new'
handleDataPost("/cluster/new") {
    //check if the user is logged in and from portal page, if yes, we can use variable `user` and `portal`
    ifFromPortalPage { user, portal ->
        //read user's request
        val request = call.readDataRequest<CreateClusterReq>()
        //this is a long-running task, we will show a progress bar to the front-end
        //there's not any data to send to front-end, so we use `Unit`, `Unit` is `Void` in kotlin 
        call.respondTraceable(OperationExecutor.addExecutorTask<Unit> {
            //open a operation scope, so that the operations under this scope will be bound to the portal automatically
            httpOperationScope(portal) {
                //initialize the progress, step 0, total step 3, name is "Creating"
                updateProgress(0, 3, "Creating")
                //use `create<Type> to create Operation, those will be bound automatically to the portal
                val id = create<CreateClusterOperation>()
                    //provide input, it is just what the front-end gives
                    .invoke(request)
                    //read the output, the only field is taskId
                    .taskId
                //update the progress, step + 1,  name is "Waiting for task"
                updateProgress("Waiting for task $id")
                //a while loop
                while (true) {
                    //another operation that check the task status
                    if (create<TraceTaskOperation>()
                            .invoke(TraceTaskReq(taskId = id))
                            .progress == 100
                    ) {
                        break
                    }
                    delay(500)
                }

                //another update    
                updateProgress("Sync")
                //equal to Thread.sleep()
                delay(3939)
                //at this stage, the task is finished, the front-end will automatically receive the response, which is <Unit> (Nothing)
            }
        })
    }
}
```

#### Sending response - Example 2

```kotlin
//register path
handleDataPost("/host/list") {
    //if the request is sent from portal page
    ifFromPortalPage { _, portal ->
        //respond traceable, the final result will be a List of HostWithDisks
        //data structure (including nested) sent to front-end must marked as `@kotlinx.serialization.Serializable`
        call.respondTraceable(OperationExecutor.addExecutorTask<List<HostWithDisks>> {
            //bind portal
            httpOperationScope(portal) {
                updateProgress(0,3,"Listing Hosts")

                //create a list
                val result = mutableListOf<HostWithDisks>()
                
                //fetched all hosts 
                val hosts =
                    create<ListHostOperation>().invoke(ListHostReq(clusterId = null, onlyFreeHosts = false)).data
                
                //fetched all disks
                for (i in hosts.indices) {
                    updateProgress(i + 1, hosts.size, "Listing disks under ${hosts[i].hostName}")
                    result.add(
                        HostWithDisks(
                            hosts[i].hostId, hosts[i].hostName, hosts[i].clusterId, hosts[i].state,
                            disks = create<ListDiskByHostOperation>().invoke(ListDiskByHostReq(hostId = hosts[i].hostId)).data
                        )
                    )
                }

                //return the list as final result
                result
            }
        })
    }
}
```

#### Other:
- For simplicity, some kotlin terminology above was changed to the one that are similar in Java and other common languages
- user data polyphonic list: refer to `polymorphic serialization with selaed class`
- code block: refer to `lambda`
- switch to another thread pool: refer to `withContext` in `Corrutine`
- `bind`: refer to `DSL builder`
- `operater function invoke(parameter)` can be called directly with `object(parameter)`


#### Projects:
- JVM - The best programming language VM
- [Kotlin](https://github.com/JetBrains/kotlin) - A modern programming language that makes developers happier.
- [IDEA](https://www.jetbrains.com/idea/) - Capable and Ergonomic IDE for JVM
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) - Kotlin multiplatform / multi-format
  **reflectionless** serialization
- [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) - A rich library for coroutines developed by
  JetBrains
- [Ktor](https://github.com/ktorio/ktor) - An asynchronous framework for creating microservices, web applications and
    more.
- [Netty](https://netty.io/) - Netty is an asynchronous event-driven network application framework
- [yamlkt](https://github.com/him188/yamlkt) - Multiplatform YAML parser & serializer for kotlinx.serialization





