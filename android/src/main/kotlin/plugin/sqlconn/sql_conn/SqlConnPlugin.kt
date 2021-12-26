package plugin.sqlconn.sql_conn

import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** SqlConnPlugin */
class SqlConnPlugin : FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    val TAG: String = "SqlConnPlugin"

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel =
            MethodChannel(flutterPluginBinding.binaryMessenger, "plugin.sqlconn.sql_conn/sql_conn")
        channel.setMethodCallHandler(this)
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

    private var connection: Connection? = null

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        if (call.method == "connectDB") {
            try {
                val ip: String = call.argument<String>("ip").toString()
                val port: String = call.argument<String>("port").toString()
                val Classes = "net.sourceforge.jtds.jdbc.Driver"
                val database = call.argument<String>("databaseName").toString()
                val username = call.argument<String>("username").toString()
                val password = call.argument<String>("password").toString()

                val url = "jdbc:jtds:sqlserver://$ip:$port/$database"

                Class.forName(Classes)
                connection = DriverManager.getConnection(url, username, password)
                result.success(true)
            } catch (e: ClassNotFoundException) {
                result.error("ClassNotFoundException", e.message.toString(), null)
            } catch (e: SQLException) {
                result.error("SQLException", e.message.toString(), null)
            } catch (e: Exception) {
                result.error("Exception", e.message.toString(), null)
            }
        } else if (call.method == "readData") {
            try {
                val query: String = call.argument<String>("query").toString()
                if (connection != null) {
                    var statement: Statement? = null
                    statement = connection!!.createStatement()
                    val resultSet: ResultSet = statement.executeQuery(query)

                    val colCount: Int =
                        resultSet.getMetaData().getColumnCount()  // <-- To get column count
                    val colNameList = arrayListOf<String>()
                    val dataList = arrayListOf<Any>()

                    for (i in 1..(colCount)) {
                        colNameList.add(resultSet.getMetaData().getColumnName(i).toString())
                    }
                    while (resultSet.next()) {

                        val stringList = arrayListOf<Any>()
                        for (j in colNameList) {
                            lateinit var data: String;
                            if (resultSet.getString(j) != null) {
                                val string = resultSet.getString(j)

                                var numeric = string.matches("-?\\d+(\\.\\d+)?".toRegex())
                                var isBoolean =
                                    if (string.toLowerCase() == "true" || string.toLowerCase() == "false") true else false
                                if (numeric) {
                                    data = string
                                } else if (isBoolean) {
                                    data = string
                                } else {
                                    data = "\"$string\""
                                }
                            } else {
                                data = "null"
                            }
                            val jString: String = "\"$j\":$data"
                            stringList.add(jString)
                        }
                        val fString: String =
                            "{" + stringList.toString().replace("[", "").replace("]", "")
                                .replace("\"null\"", "null") + "}"
                        dataList.add(fString)
                    }
                    result.success(dataList.toString())
                } else {
                    throw Exception("Database is not connected")
                }
            } catch (e: Exception) {
                result.error("Exception", e.message.toString(), null)
            }
        } else if (call.method == "writeData") {
            try {
                val query: String = call.argument<String>("query").toString()
                if (connection != null) {
                    var statement: Statement? = null
                    statement = connection!!.createStatement()
                  statement.execute(query)
                    result.success(true)
                } else {
                    throw Exception("Database is not connected")
                }
            } catch (e: Exception) {
                result.error("Exception", e.message.toString(), null)
            }
        } else if(call.method == "disconnectDB"){
            try {
                if (connection != null) {
                    android.util.Log.i(TAG, "onDetachedFromEngine: Closing SQL Connection")
                    connection!!.close()
                    result.success(false)
                }else{
                    throw  Exception("Database is not connected")
                }
            }catch ( e:Exception){
                result.error("Exception",e.message.toString(),null)
            }
        }
        else {
            android.util.Log.i(TAG, "onMethodCall: NO onMethodCall FOUND!")
            result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        if (connection != null) {
                  connection!!.close()
        }
    }

}
