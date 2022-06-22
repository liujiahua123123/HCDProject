package server.route

import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import operation.host.ListHostOperation
import operation.host.ListHostReq
import org.apache.sshd.client.SshClient
import server.handleDataPost
import server.ifFromPortalPage
import server.respondTraceable
import ssh.HCDSshClient
import utils.OperationExecutor

fun Routing.globalRoute() {

    handleDataPost("/purge-db"){
        ifFromPortalPage { user, portal ->
            call.respondTraceable(OperationExecutor.addExecutorTask<Unit> {
                updateProgress(0,1,"Allocating Host connection points")
                val address = ListHostOperation().apply { this.portal=portal }.invoke(ListHostReq()).data

                updateProgress(1,address.size + 4,"Making SSH Connections")
                val list = address.map { HCDSshClient(
                    name = it.hostName,
                    address = it.managementAddress!!
                ) }

                updateProgress("Disabling Services (Concurrent)")
                supervisorScope {
                    list.forEach {
                        launch {
                            it.execute("sudo systemctl stop hcdmgmt")
                            it.execute("sudo systemctl stop hcdadmin")
                        }
                    }
                }
                delay(5000)


                list.forEach {
                    updateProgress("Running db_purge.sh on " + it.name)
                    it.execute("sudo /usr/share/hcdinstall/scripts/db_purge.sh")
                }
                delay(5000)

                updateProgress("Running db_config.sh (Concurrent)")
                supervisorScope {
                    list.forEach {
                        launch {
                            it.execute("sudo /usr/share/hcdinstall/scripts/db_config.sh")
                        }
                    }
                }
                delay(5000)

                updateProgress("Starting All Services")
                supervisorScope {
                    list.forEach {
                        launch {
                            it.execute("sudo systemctl start hcdmgmt")
                            it.execute("sudo systemctl start hcdadmin")
                        }
                    }
                }
                delay(8000)
            })
        }
    }


}