import fi.iki.elonen.NanoHTTPD
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.YAMLException
import java.io.FileInputStream
import java.io.IOException
import java.net.BindException
import java.security.KeyStore
import java.util.*
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLServerSocketFactory
import kotlin.collections.HashMap
import kotlin.system.exitProcess

class LittleServer
{
    companion object {
        fun laodConfig(baseDir: File2, configFile: File2): AppConfig {
            val configYaml = Yaml().load(configFile.content) as HashMap<String, Any>

            return AppConfig(
                baseDir = baseDir,
                configYaml = configYaml,
                host = configYaml["host"]?.run { this as String } ?: "0.0.0.0",
                port = configYaml["port"]?.run { this as Int } ?: 8850,
                performanceMode = configYaml["performance-mode"]?.run { this as Boolean } ?: false,
                certificateFile = configYaml["jks-certificate-file"]?.run { this as String } ?: "",
                certificatePass = configYaml["jks-certificate-pass"]?.run { this as String } ?: "",
            )
        }

        fun exitWithError(message: String): Nothing
        {
            println(message)
            exitProcess(1)
        }

        fun loadCertificate(certificateFile: String, certificatePass: String): SSLServerSocketFactory
        {
            if (File2(certificateFile).exists)
            {
                val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
                val keystoreStream = FileInputStream(certificateFile)

                try {
                    keystore.load(keystoreStream, certificatePass.toCharArray())
                } catch (e: IOException) {
                    println("SSL证书密码不正确")
                    exitProcess(1)
                }

                val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                keyManagerFactory.init(keystore, certificatePass.toCharArray())
                return NanoHTTPD.makeSSLSocketFactory(keystore, keyManagerFactory)
            } else {
                println("SSL证书文件找不到: $certificateFile")
                exitProcess(1)
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val baseDir = File2(System.getProperty("user.dir"))
            val configFile = baseDir + "config.yml"

            if(!baseDir.exists)
                exitWithError("找不到工作目录: ${baseDir.path}")

            if(!configFile.exists)
                exitWithError("找不到配置文件: ${configFile.path}")

            val config: AppConfig
            val server: Server

            try {
                config = laodConfig(baseDir, configFile)
            } catch (e: YAMLException) {
                exitWithError("配置文件读取出错(格式不正确)，位置和原因: ${e.cause?.message}")
            }

            try {
                server = Server(config)

                if (config.certificateFile.isNotEmpty() && config.certificatePass.isNotEmpty())
                {
                    server.makeSecure(loadCertificate(config.certificateFile, config.certificatePass), null)
                    println("SSL证书已加载")
                }

                if (config.performanceMode)
                {
                    println("高性能模式已经开启，正在生成res目录的缓存...")
                    server.regenDirStructureInfoCache()
                    println("缓存生成完毕")
                }

                server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)

                val host = config.host
                val port = config.port

                println("Listening on: $host:$port")
                println("API地址: http://$host:$port/index.json")

                println()
                if (config.performanceMode)
                    println("使用提示1：修改更新规则需要退出程序之后修改，修改res目录下的文件之后请重启或者使用reload指令来重新加载")
                else
                    println("使用提示1：修改更新规则需要退出程序之后修改，修改res目录下的文件会实时生效，不用退出程序")
                println("使用提示2：显示的所有报错信息都不用管，直接忽略就好！")
                println("使用提示3：可用指令：")
                println("              stop或者s：退出程序")
                println("              reload或者r：重新生成res目录的缓存（仅开启高性能时有效）")
            } catch (e: BindException) {
                println("端口监听失败，可能是端口冲突，原因: ${e.message}")
                exitProcess(1)
            }

            // 读取控制台输入
            val scanner = Scanner(System.`in`)
            while (true)
            {
                val line = scanner.nextLine()
                if (line == "stop" || line == "s")
                {
                    exitProcess(1)
                } else if (line == "reload" || line == "r") {
                    println("正在重新生成res目录的缓存...")
                    server.regenDirStructureInfoCache()
                    println("缓存生成完毕")
                }

            }
        }
    }
}