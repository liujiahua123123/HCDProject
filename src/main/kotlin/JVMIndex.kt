import kotlinx.coroutines.runBlocking
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class JVMIndex {
    companion object{
        @JvmStatic
        fun main(args: Array<String>){
            runBlocking {
                ktmain()
            }
        }
    }
}