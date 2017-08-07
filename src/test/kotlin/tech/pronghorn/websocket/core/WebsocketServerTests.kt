package tech.pronghorn.websocket.core

import tech.pronghorn.stats.StatTracker
import tech.pronghorn.websocket.protocol.*
import tech.pronghorn.server.*
import tech.pronghorn.server.config.WebsocketClientConfig
import tech.pronghorn.server.config.WebServerConfig
import tech.pronghorn.coroutines.service.Service
import mu.KotlinLogging
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

import graphql.Scalars.*
import graphql.*
import graphql.execution.SimpleExecutionStrategy
import graphql.schema.GraphQLFieldDefinition.newFieldDefinition
import graphql.schema.GraphQLObjectType.newObject
import graphql.schema.*
import java.sql.ResultSet
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

/*
object GraphQLParser {
    // TODO: make this tail recursive via implementing the instance function here since it's the same for everything
//    fun fieldsFromSelectionSet<T : FetchableField>(fieldSet: FieldSet<T>, selectionSet: SelectionSet): Set[T] {
//        val typeFields = mutable.HashSet[T]()
//        val iter = selectionSet.getSelections.iterator
//        while (iter.hasNext) {
//            iter.next match {
//                case graphQLField: Field =>
//                val typeField = fieldSet.getField(graphQLField)
//                typeFields += typeField.asInstanceOf[T]
//
//                typeField match {
//                    case fetchable: FetchableSubObject[_, _] =>
//                    val subFields = fieldsFromSelectionSet(fetchable.child, graphQLField.getSelectionSet)
//                    typeFields += fetchable(subFields).asInstanceOf[T]
//                    case _ =>
//                }
//            }
//        }
//        typeFields
//    }
}

// TODO: DataFetcher used to not take a type, now it does, should be ... something
object HostFetcher : DataFetcher<Any> {
    override fun get(environment: DataFetchingEnvironment): Any {
        println("HostFetcher.get()")
        /*val fields = GraphQLParser.fieldsFromSelectionSet(Host, environment.getFields.get(0).getSelectionSet)
        //        import monix.execution.Scheduler.Implicits.global
        val data = HostData()
        val task = data.getAll(fields).map(_.head)
        //        val result = Await.result(task.runAsync, Duration(10, TimeUnit.SECONDS)).head
        //        println(s"RESULT: $result")
        //        result
        task*/
        TODO()
    }
}

interface FetchableField : DataFetcher<Any> {
    fun getTable(): FieldSet<*>
    val name: String

    override fun get(environment: DataFetchingEnvironment): Any = {
        TODO()
//        val source = environment.source
//        if (source != null && source.isInstanceOf[Map[_, _]]) {
//            val result = source.asInstanceOf[Map[FetchableField, _]](this).asInstanceOf[AnyRef]
//            result match {
//                case buf: Seq[_] => buf.asJava
//                case other => other
//            }
//        }
//        else {
//            null
//        }
    }
}

interface TypedSQLField<T> {
    val name: String

    fun getValue(resultSet: ResultSet,
                 columnIndex: Int): T
}

interface SQLField {
    fun getTable(): FieldSet<*>
}

interface SQLStringField : SQLField, TypedSQLField<String> {
    override fun getValue(resultSet: ResultSet,
                          columnIndex: Int): String = resultSet.getString(columnIndex + 1)
}

interface SQLLongField : SQLField, TypedSQLField<Long> {
    override fun getValue(resultSet: ResultSet,
                          columnIndex: Int): Long = resultSet.getLong(columnIndex + 1)
}

interface SQLIntField : SQLField, TypedSQLField<Int> {
    override fun getValue(resultSet: ResultSet,
                          columnIndex: Int): Int = resultSet.getInt(columnIndex + 1)
}

interface SQLBooleanField : SQLField, TypedSQLField<Boolean> {
    override fun getValue(resultSet: ResultSet,
                          columnIndex: Int): Boolean = resultSet.getBoolean(columnIndex + 1)
}


interface HostField : FetchableField {
    override fun getTable(): FieldSet<*> = Host
}

//abstract class HostSQLField<T>(override val name: String) : SQLField<T>, HostField {
//    override fun getTable(): SQLTable<*> = Host
//}

abstract class FieldSet<T : FetchableField>(val tableName: String) {
    abstract val allFields: Set<T>

    abstract fun sqlOnly(fields: Set<T>): Set<SQLField>

//    private lazy val fieldsByName = allFields.map { field =>
//        field.toString -> field
//    }.toMap

    fun getField(field: graphql.language.Field): T {
        //fieldsByName.getOrElse(field.getName, throw new Exception(s"Could not find field ${field.getName}"))
        TODO()
    }

    override fun toString(): String = tableName
}


interface FetchableType<T>

interface IDType

abstract class Field(val name: String,
                     val isSQL: Boolean,
                     val graphql: GraphQLType)

object HostGroup : FieldSet<HostGroupField>("host_groups") {
    class ID : IDType

    override val allFields: Set<HostGroupField> = setOf()

    override fun sqlOnly(fields: Set<HostGroupField>): Set<SQLField> {
        return setOf()
    }

    val id: HostGroupField = object : HostGroupField("host_group_id"), SQLStringField, FetchableType<HostGroup.ID> {}
}

object HostNetwork : FieldSet<HostNetworkField>("host_networks") {
    class ID : IDType

    override val allFields: Set<HostNetworkField> = setOf()

    override fun sqlOnly(fields: Set<HostNetworkField>): Set<SQLField> {
        return setOf()
    }

    val id: HostNetworkField = object : HostNetworkField("host_network_id"), SQLStringField, FetchableType<HostNetwork.ID> {}
    val hostID: HostNetworkField = object : HostNetworkField("host_id"), SQLStringField, FetchableType<Host.ID> {}
}

abstract class HostNetworkField(override val name: String) : FetchableField {
    override fun getTable(): FieldSet<*> = HostNetwork
}

abstract class HostGroupField(override val name: String) : FetchableField {
    override fun getTable(): FieldSet<*> = HostNetwork
}

interface BasicTmpField {
    val name: String
    val table: FieldSet<*>
}

interface TmpField<A, B> {
    fun getValue(resultSet: ResultSet,
                 columnIndex: Int): B
}

interface SQLTmpField {

}

interface SQLStringTmpField : SQLTmpField, TmpField<String, String> {
    override fun getValue(resultSet: ResultSet,
                          columnIndex: Int): String = resultSet.getString(columnIndex + 1)
}

interface IDTmpField<B : TmpIDType> : TmpField<String, B> {

}

class TmpHostField : BasicTmpField {
    override val name: String = "hosts"
    override val table: FieldSet<*> = Host
}

abstract class TmpIDType {
    abstract val uuid: UUID
}

annotation class GraphQLSQLName(val name: String)

annotation class GraphQLDependsOn(val name: String)

data class JustHost(val id: Host.ID,
                    val hostname: String,
                    val name: String,
                    @GraphQLSQLName("host_group_id")
                    val hostGroupID: HostGroup.ID,
                    @GraphQLDependsOn("hostGroupID")
                    val hostGroup: HostGroup?,
                    val networks: List<HostNetwork>?)

object Host : FieldSet<HostField>("hosts") {
    data class ID(override val uuid: UUID) : TmpIDType()

    val id = object : IDTmpField<Host.ID> {
        override fun getValue(resultSet: ResultSet, columnIndex: Int): Host.ID = Host.ID(UUID.fromString(resultSet.getString(columnIndex + 1)))
    }
    val hostname = object : SQLStringTmpField { val name = "hostname"}
    val name = object : SQLStringTmpField { val name = "name"}
    val hostGroupID = object : IDTmpField<Host.ID> {
        override fun getValue(resultSet: ResultSet, columnIndex: Int): Host.ID = Host.ID(UUID.fromString(resultSet.getString(columnIndex + 1)))
    }

//    val id: HostField = object : HostField, SQLStringField, FetchableType<Host.ID> { override val name: String  = "host_id" }
//    val hostname: HostField = object : HostField("hostname"), SQLStringField, FetchableType<String> {}
//    val name: HostField = object : HostField("name"), SQLStringField, FetchableType<String> {}
//    val hostGroupID: HostField = object : HostField("host_group_id"), SQLStringField, FetchableType<HostGroup.ID?> {}
    //val hostGroup: HostField = object : HostField("host_group"), SubObjectFields(hostGroup), HostField
//    val hostGroup: HostField = object : HostField("host_group"), OptionalSingleSubObject<HostField, HostGroupField>(Host, HostGroup, Host.hostGroupID, HostGroup.id) {}

    //val networks: HostField = object : HostField("networks"), SubObjectFields<HostField, HostNetworkField>(networksSub) {}
    //val networksSub: FetchableSubObject<HostField, HostNetworkField> = object: MultipleSubObject<HostField, HostNetworkField>(Host, HostNetwork, Host.id, HostNetwork.hostID) {}

//    case
//    class networks(fields: Set[HostNetworkField]) extends SubObjectFields(networks) with HostField
//    case
//    object networks extends MultipleSubObject(Host, HostNetwork, Host.id, HostNetwork.hostID) with HostField
//    case
//    class hostGroup(fields: Set[HostGroupField]) extends SubObjectFields(hostGroup) with HostField
//    case
//    object hostGroup extends OptionalSingleSubObject(Host, HostGroup, Host.hostGroupID, HostGroup.id) with HostField
//    case
//    class roles(fields: Set[RoleField]) extends SubObjectFields(roles) with HostField
//    case
//    object roles extends MultipleSubObject(Host, HostRole, Host.id, HostRole.hostID) with HostField

    override val allFields: Set<HostField> = setOf(/*id*/ /*, name, hostname, hostGroupID*//*, networks, hostGroup, roles*/)

    @Suppress("UNCHECKED_CAST")
    override fun sqlOnly(fields: Set<HostField>): Set<SQLField> {
        val sql: Set<SQLField> = fields.filter { field -> field is SQLField }.toSet() as Set<SQLField>
        return (sql + id) as Set<SQLField>
        //val fieldSet = sql + id ++ (if (fields.contains(Host.hostGroup)) Set(Host.hostGroupID) else Set.empty)
    }
}

interface FetchableSubObject<P : FetchableField, C : FetchableField> {
    val parent: FieldSet<P>
    val child: FieldSet<C>
    val parentField: P
    val childField: C

    fun apply(fields: Set<C>): SubObjectFields<P, C>
}

abstract class MultipleSubObject<P : FetchableField, C : FetchableField>(override val parent: FieldSet<P>,
                                                                         override val child: FieldSet<C>,
                                                                         override val parentField: P,
                                                                         override val childField: C) : FetchableSubObject<P, C>

interface SingleSubObject<P : FetchableField, C : FetchableField> : FetchableSubObject<P, C> {
    val isOptional: Boolean
}

abstract class RequiredSingleSubObject<P : FetchableField, C : FetchableField>(
        override val parent: FieldSet<P>,
        override val child: FieldSet<C>,
        override val parentField: P,
        override val childField: C) : SingleSubObject<P, C> {
    override val isOptional = false
}

abstract class OptionalSingleSubObject<P : FetchableField, C : FetchableField>(
        override val parent: FieldSet<P>,
        override val child: FieldSet<C>,
        override val parentField: P,
        override val childField: C) : SingleSubObject<P, C> {
    override val isOptional = true
}

abstract class SubObjectFields<P : FetchableField, C : FetchableField>(
        val subObject: FetchableSubObject<P, C>) {
    abstract val fields: Set<C>
}

enum class FieldWrapper {
    NONNULL,
    LIST,
    OPTIONAL
}

private fun getOutputType(type: KType): GraphQLOutputType {
    val erased = type.jvmErasure
    return when (erased) {
        Long::class -> GraphQLLong
        String::class -> GraphQLString
        Boolean::class -> GraphQLBoolean
        else -> {
            if (erased.isSubclassOf(IDType::class)) {
                GraphQLID
            }
            else {
                GraphQLTypeReference(erased.simpleName!!)
            }
        }
    }
}

private fun getFieldType(type: KType,
                         wrapper: FieldWrapper = FieldWrapper.NONNULL): GraphQLOutputType {
    val outputType = getOutputType(type)

    if (type.isMarkedNullable) {
        return outputType
    }
    else {
        return GraphQLNonNull(outputType)
    }

//    val cls = type.jvmErasure
//    if(cls.isSubclassOf(Set::class) || cls.isSubclassOf(List::class)){
//        val superCls = cls.superclasses.find { s -> s.isSubclassOf(Collection::class) }!!
//        return getFieldType(, FieldWrapper.LIST)
//    }
}

fun <T : FetchableField> altRegisterType(fieldSet: FieldSet<T>): GraphQLObjectType {
    val fields = java.util.ArrayList<GraphQLFieldDefinition>()

//        val validClasses = Set[Type](
//            classOf[FetchableType[_]],
//            classOf[SingleSubObject[_, _]],
//            classOf[MultipleSubObject[_, _]]
//        )

    fieldSet.allFields.forEach { field ->
        when (field) {
//            is SingleSubObject<*,*> -> TODO()
            is FetchableType<*> -> {
                val fetchableType = field::class.supertypes.find { type -> type.jvmErasure == FetchableType::class }!!.arguments.first().type!!
                val fieldType = getFieldType(fetchableType)
                fields.add(newFieldDefinition().name(field.name).dataFetcher(field).type(fieldType).build())
            }
            else -> println("Field $field not parsed as anything!?")
        }
    }
//
//    fieldSet.allFields.foreach {
//        field =>
//        field match {
//            case single : SingleSubObject [_, _] =>
////                    println(s"Field ${field.toString} parsed as SingleSubObject")
//            val wrapper = if (single.isOptional) FieldWrapper.OPTIONAL else FieldWrapper.NONNULL
//            val fieldType = getFieldType(single.child.getClass, wrapper)
//            fields.add(newFieldDefinition().name(field.toString).dataFetcher(field).`type`(fieldType).build)
//            case multi : MultipleSubObject [_, _] =>
////                    println(s"Field ${field.toString} parsed as MultipleSubObject")
//            val fieldType = getFieldType(multi.child.getClass, FieldWrapper.LIST)
//            fields.add(newFieldDefinition().name(field.toString).dataFetcher(field).`type`(fieldType).build)
//            case fetchable : FetchableType [_] =>
////                    println(s"Field ${field.toString} parsed as FetchableType")
//            val param = field.getClass.getGenericInterfaces.find(_.getTypeName.contains("FetchableType")).get.asInstanceOf[ParameterizedType]
//            val fieldType = getFieldType(param.getActualTypeArguments.head)
//            fields.add(newFieldDefinition().name(field.toString).dataFetcher(field).`type`(fieldType).build)
//            case _ =>
////                    println(s"Field ${field.toString} not parsed as anything!?")
//            ???
//        }


//            field.getClass.getGenericInterfaces.foreach {
//                case param: ParameterizedType =>
//                    if (validClasses.contains(param.getRawType)) {
//                        field match {
//                            case single: SingleSubObject[_, _] =>
//                                val wrapper = if (single.isOptional) FieldWrapper.OPTIONAL else FieldWrapper.NONNULL
//                                val fieldType = getFieldType(single.subObject.getClass, wrapper)
//                                fields.add(newFieldDefinition().name(field.toString).dataFetcher(field).`type`(fieldType).build)
//                            case multi: MultipleSubObject[_, _] =>
//                                val fieldType = getFieldType(multi.subObject.getClass, FieldWrapper.LIST)
//                                fields.add(newFieldDefinition().name(field.toString).dataFetcher(field).`type`(fieldType).build)
//                            case fetchable: FetchableType[_] =>
//                                val fieldType = getFieldType(param.getActualTypeArguments.head)
//                                fields.add(newFieldDefinition().name(field.toString).dataFetcher(field).`type`(fieldType).build)
//                            case _ =>
//                        }
//                    }
//                case _ =>
//            }
//    }

    val objectType = newObject().name(fieldSet::class.simpleName!!).fields(fields).build()
    return objectType
}

class CounterHandler : FrameHandler() {
    val stats = StatTracker()
    var framesHandled = 0L

    override suspend fun handleTextFrame(frame: TextFrame) {
        val now = System.nanoTime()
        stats.addValue(now - frame.text.toLong())
        framesHandled += 1
    }

    val hostType = altRegisterType(Host)

//    init {
//        println(hostType)
//        hostType.fieldDefinitions.forEach { field ->
//            println("${field.name} : ${field.type}")
//            val fieldType = field.type
//            when (fieldType) {
//                is GraphQLTypeReference -> println(fieldType.name)
//                is GraphQLNonNull -> {
//                    val wrappedType = fieldType.wrappedType
//                    when (wrappedType) {
//                        is GraphQLTypeReference -> println(wrappedType.name)
//                    }
//                }
//            }
//        }
//        System.exit(1)
//    }

    val hostsQuery = newFieldDefinition().type(hostType).name("host").dataFetcher(HostFetcher).build()
//    val multiHostsQuery = newFieldDefinition().type(GraphQLList(TODO())).name("hosts").dataFetcher(HostsFetcher).build

    val apiRoot = newObject().name("root")
            .fields(listOf(hostsQuery/*, multiHostsQuery*/))
            .build()

    val schema = GraphQLSchema.newSchema().query(apiRoot).build(setOf(
            hostType/*, hostNetworkType, hostGroupType, hostRoleType, roleHostType, roleType*/
    ))

    val query = "{host{id}}"

    val executed: ExecutionResult = GraphQL(schema, SimpleExecutionStrategy()).execute(query)

/*    override suspend fun handleTextFrame(frame: TextFrame) {
        println(executed.errors)
        System.exit(1)
//        val request = parseGraphQLRequest(frame)
        framesHandled += 1
    }*/

    override suspend fun handlePingFrame(frame: PingFrame) {}
    override suspend fun handlePongFrame(frame: PongFrame) {}
    override suspend fun handleCloseFrame(frame: CloseFrame) {}
    override suspend fun handleBinaryFrame(frame: BinaryFrame) {}
}

class FakeConnection(fakeWorker: WebWorker,
                     fakeSocket: SocketChannel,
                     fakeKey: SelectionKey) : HttpConnection(fakeWorker, fakeSocket, fakeKey) {
    override fun handleHandshakeRequest(request: ParsedHttpRequest, handshaker: WebsocketHandshaker): Boolean = true
    override val shouldSendMasked: Boolean = false
    override val requiresMasked: Boolean = false
}

class WebsocketServerTests : CDBTest() {
    val host = "localhost"
    val port = 5432
    val address = InetSocketAddress(host, port)

    val noopFrameHandler = object : FrameHandler() {
        override suspend fun handleCloseFrame(frame: CloseFrame): Unit {}
        override suspend fun handlePongFrame(frame: PongFrame): Unit {}
        override suspend fun handleBinaryFrame(frame: BinaryFrame): Unit {}
        override suspend fun handleTextFrame(frame: TextFrame): Unit {}
        override suspend fun handlePingFrame(frame: PingFrame): Unit {}
    }

    val handshaker = WebsocketHandshaker()

    fun sendHandshake(socket: SocketChannel) {
        val keyBytes = ByteArray(16)
        Random().nextBytes(keyBytes)
        val clientHandshake = handshaker.getClientHandshakeRequest(host, keyBytes)

        val writer = OutputStreamWriter(socket.socket().getOutputStream(), StandardCharsets.UTF_8)
        writer.write(clientHandshake)
        writer.flush()
    }

    val noopConfig = WebServerConfig(address,/* { noopFrameHandler },*/ 1)

    init {
        "servers" should "accept incoming connections" {
            repeat(0) {
                val server = WebServerWorker(noopConfig)
                server.start()
                eventually { server.isRunning shouldBe true }
                try {
                    val clientCount = 1 + Random().nextInt(16)
                    for (c in 1..clientCount) {
                        SocketChannel.open(address)
                    }

                    eventually(5.seconds) { server.getActiveConnectionCount() shouldBe clientCount }
                }
                finally {
                    server.shutdown()
                }
            }
        }

        "servers" should "handshake successfully" {
            repeat(0) {
                val server = WebServerWorker(noopConfig)
                server.start()
                try {
                    eventually { server.isRunning shouldBe true }
                    val channel = SocketChannel.open(address)
                    eventually { server.getPendingConnectionCount() shouldBe 1 }
                    sendHandshake(channel)
                    eventually {

                        server.getPendingConnectionCount() shouldBe 0
                        server.getActiveConnectionCount() shouldBe 1
                    }
                }
                finally {
                    server.shutdown()
                }
            }
        }

        "servers" should "send websocket frames to the frame handler" {
            var totalConns = 0
            repeat(16) {
                val serverThreadCount = 8
                val clientThreadCount = 4
                val channelCount = 4

                val batchSize = 1024
                val batchCount = 128 * 16

                val counterHandlers = mutableListOf<CounterHandler>()
                val serverConfig = WebServerConfig(address, serverThreadCount)

                val server = WebServer(serverConfig)
                server.start()

                val channels = mutableListOf<SocketChannel>()
                try {
                    eventually { server.isRunning shouldBe true }

                    println(totalConns)
                    for (c in 1..channelCount) {
                        totalConns++
                        val channel = SocketChannel.open(address)
                        channel.socket().setKeepAlive(true)
                        channels.add(channel)
                    }

                    eventually(5.seconds) { server.getPendingConnectionCount() shouldBe channelCount }
                    channels.forEach { channel -> sendHandshake(channel) }
                    eventually(5.seconds) { server.getActiveConnectionCount() shouldBe channelCount }
                    server.getPendingConnectionCount() shouldBe 0

                    val maskBytes = ByteArray(4)
                    val fakeWorker = object : WebWorker() {
                        override val logger = KotlinLogging.logger {}
                        override fun services(): List<Service> = emptyList()
                        override fun processKey(key: SelectionKey) = Unit
                    }
                    val fakeSocket = SocketChannel.open()
                    fakeSocket.configureBlocking(false)
                    val fakeSelector = Selector.open()
                    val fakeKey = fakeSocket.register(fakeSelector, SelectionKey.OP_READ)
                    val fakeConnection = FakeConnection(fakeWorker, fakeSocket, fakeKey)

                    Random().nextBytes(maskBytes)
                    var frame = TextFrame("${System.nanoTime()}", fakeConnection)
                    val frameSize = frame.getEncodedLength(true)

                    val clientThreads = mutableListOf<Thread>()

                    for (c in 1..clientThreadCount) {
                        val clientThread = thread(start = false) {
                            val clientID = c - 1
                            val buffer = ByteBuffer.allocate(batchSize * frameSize)
                            var y = 0
                            while (y < batchCount) {
                                var x = 0

                                val id = ((y % (channelCount / clientThreadCount)) * clientThreadCount) + clientID

                                while (x < batchSize) {
                                    frame = TextFrame("${System.nanoTime()}", fakeConnection)
                                    FrameWriter.encodeMaskedFrame(frame, maskBytes, buffer, buffer.position())
                                    x += 1
                                }
                                buffer.flip()
                                val wrote = channels[id].write(buffer)
                                wrote shouldBe (frameSize * batchSize)
                                buffer.clear()

                                y += 1
                            }
                        }
                        clientThreads.add(clientThread)

                    }

                    val totalExpected = (batchSize * batchCount * clientThreadCount)

                    val taken = measureTimeMillis {
                        clientThreads.forEach(Thread::start)
                        clientThreads.forEach(Thread::join)
                        eventually(5.seconds) { counterHandlers.map { handler -> handler.framesHandled }.sum() shouldBe totalExpected.toLong() }
                    }

                    fakeWorker.shutdown()
                    fakeSelector.close()
                    fakeSocket.close()
                    try {
                        fakeConnection.close("done")
                    }
                    catch (ex: AssertionError) {
                        // ignore
                    }
                    counterHandlers.map { handler ->
                        println("End to end latency: min ${handler.stats.minMillis()} avg ${handler.stats.meanMillis()}")
//                        handler.stats.printHistogram()
                    }
                    val fps = (1000f / taken) * totalExpected
                    val bandwidth = (fps * frameSize) / (1024 * 1024)
                    logger.warn("Took $taken ms for $totalExpected frames. Effective fps : $fps, Effective bandwidth: $bandwidth MB/s")
                }
                catch (ex: AssertionError) {
                    ex.printStackTrace()
                }
                finally {
                    server.shutdown()
                    channels.forEach { it.close() }
                }
            }
        }

        "servers" should "send websocket frames to the frame handler from real clients" {
            repeat(0) {
                val serverThreadCount = 1
                val clientThreadCount = 1
                val channelCount = 256

                val batchSize = 512
                val batchCount = 128 * 16

                val counterHandlers = mutableListOf<CounterHandler>()
                val serverConfig = WebServerConfig(address, serverThreadCount)

                val server = WebServer(serverConfig)
                server.start()
                eventually { server.isRunning shouldBe true }

                val clientConfig = WebsocketClientConfig(noopFrameHandler, clientThreadCount)
                val client = WebsocketClient(clientConfig)
                val futConnection = client.connect(address)

                try {
                    thread {
                        Thread.sleep(400)
//                        futConnection.cancel()
                    }

                    println("After")
//                    when(connection.get()) {
//                        is HttpClientConnection -> Unit
//                        else -> fail("Connection future did not return a connection.")
//                    }

                    eventually {
                        client.getActiveConnectionCount() shouldBe 1
                        server.getActiveConnectionCount() shouldBe 1
                    }
                }
                finally {
                    println("Server pending: ${server.getPendingConnectionCount()}, client pending: ${client.getPendingConnectionCount()}")
                    try {
                        server.shutdown()
                        client.shutdown()
                    }
                    catch(ex: Exception) {
                        logger.error("EXCEPTION IN SHUTDOWN ${ex.message}")
                    }
                }
            }
        }

        /*"servers" should "send websocket frames to the frame handler" {
            repeat(16) {
                val stats = DescriptiveStatistics(1024 * 1024)
                var framesHandled = 0
                val counterHandler = object : FrameHandler() {
                    override fun handleTextFrame(frame: TextFrame*//*, connection: HttpConnection*//*) {
                        val now = System.currentTimeMillis()
                        stats.addValue((now - frame.text.toLong()).toDouble())
                        framesHandled += 1
                    }

                    override fun handlePingFrame(frame: PingFrame*//*, connection: HttpConnection*//*) {}
                    override fun handlePongFrame(frame: PongFrame*//*, connection: HttpConnection*//*) {}
                    override fun handleCloseFrame(frame: CloseFrame*//*, connection: HttpConnection*//*) {}
                    override fun handleBinaryFrame(frame: BinaryFrame*//*, connection: HttpConnection*//*) {}
                }

                val server = WebServerWorker(address, counterHandler)
                val thread = thread { server.start() }

                try {
                    eventually { server.isRunning shouldBe true }
                    val channel = SocketChannel.open(address)
                    eventually { server.getPendingConnectionCount() shouldBe 1 }

                    sendHandshake(channel)
                    eventually { server.getActiveConnectionCount() shouldBe 1 }

                    val maskBytes = ByteArray(4)
                    Random().nextBytes(maskBytes)
                    var frame = TextFrame("${System.currentTimeMillis()}")
                    val frameSize = frame.getEncodedLength(true)
                    val batchSize = 512
                    val batchCount = 512 * 4
                    val buffer = ByteBuffer.allocate(batchSize * frameSize)

                    val taken = measureTimeMillis {
                        var x = 0
                        var y = 0
                        while (y < batchCount) {
                            while (x < batchSize) {
                                frame = TextFrame("${System.currentTimeMillis()}")
                                FrameWriter.encodeMaskedFrame(frame, maskBytes, buffer, buffer.position())
                                x += 1
                            }
                            buffer.flip()
                            val wrote = channel.write(buffer)
                            wrote shouldBe (frameSize * batchSize)
                            buffer.clear()
                            x = 0
                            y += 1
                        }
                        eventually { framesHandled shouldBe batchSize * batchCount }
                    }

                    println("Took $taken ms for ${batchSize * batchCount} frames. Effective fps : ${(1000f / taken) * (batchSize * batchCount)}")
                    println("End to end latency: min ${Math.round(stats.min)} avg ${Math.round(stats.mean)}, 99th % ${Math.round(stats.getPercentile(99.0))}")
                } finally {
                    server.requestShutdown()
                    thread.join()
                }
            }
        }*/
    }
}

/*
class WebsocketClientTests : FlatSpec(), Eventually {

    fun runProcess(manager: WebsocketManager<*>): Unit {
        val handler = manager.processQueue.poll()
        handler?.process()
    }

    fun runSingleProcess(manager: WebsocketManager<*>): Unit {
        eventually(Duration(5, TimeUnit.SECONDS)) {
            val handler = manager.processQueue.poll()

            if (handler != null) {
                try {
                    handler.process()
                } catch(ex: Exception) {
                    ex.printStackTrace()
                }
            } else {
                throw Exception("Handler is null.")
            }
        }
    }

    val host = "localhost"
    val port = 5432
    val address = InetSocketAddress(host, port)
    val noopFrameHandler = object : FrameHandler() {
        override fun handleCloseFrame(frame: CloseFrame, connection: HttpConnection): Unit {}
        override fun handlePongFrame(frame: PongFrame, connection: HttpConnection): Unit {}
        override fun handleBinaryFrame(frame: BinaryFrame, connection: HttpConnection): Unit {}
        override fun handleTextFrame(frame: TextFrame, connection: HttpConnection): Unit {}
        override fun handlePingFrame(frame: PingFrame, connection: HttpConnection): Unit {}
    }

    fun withServerAndClient(serverFrameHandler: FrameHandler,
                            clientFrameHandler: FrameHandler,
                            block: (WebServerWorker, WebsocketClient) -> Unit): Unit {
        val server = WebServerWorker (address, serverFrameHandler)
        server.start()

        val randomGenerator = XoRoShiRo128PlusRandom (Util.randomSeed())
        val client = WebsocketClient (clientFrameHandler, randomGenerator)
        client.start()

        try {
            block(server, client)
        } finally {
            println("Shutting down client")
            client.shutdown()
            println("Shutting down server")
            server.shutdown()
            println("Shutdown complete")
        }
    }

    init {
        "WebsocketClient" should "complete its future when the server accept's it's connection" {
            kotlin.repeat(512, {
                val serverSocket = ServerSocketChannel.open()
                serverSocket.bind(address)

                val client = WebsocketClient(noopFrameHandler)
                client.start()
                val futConnection = client.connect(address)
                futConnection.isDone shouldBe false
                serverSocket.accept()

                runSingleProcess(client) // Connect
                futConnection.isDone shouldBe true
                serverSocket.close()
            })
        }

        "WebsocketClient" should "successfully handshake" {
            withServerAndClient(noopFrameHandler, noopFrameHandler, { server, client ->
                val futConnection = client.connect(address)
                runSingleProcess(server) // Accept
                runSingleProcess(client) // Connect

                val clientConnection = futConnection.get()
                val serverConnection = server.getConnections().first()

                clientConnection.isHandshakeCompleted shouldBe false
                serverConnection.isHandshakeCompleted shouldBe false

                runSingleProcess(server) // Process Handshake Request
                serverConnection.isHandshakeCompleted shouldBe true

                runSingleProcess(client) // Process Handshake Response
                clientConnection.isHandshakeCompleted shouldBe true
            })
        }
    }
/*


    "HttpClientConnection" should "be capable of sending frames" in repeat(512) {
        withServerAndClient(noopFrameHandler, noopFrameHandler) { (server, client) =>
            val futConnection = client.connect(address)
            runSingleProcess(server) // Accept
            runSingleProcess(client) // Connect
            runSingleProcess(server) // Process Handshake Request
            runSingleProcess(client) // Process Handshake Response

            val clientConnection = futConnection.get
            val serverConnection = server.getConnections.head

            val otherFutConnection = client.connect(address)
            runSingleProcess(server) // Accept
            runSingleProcess(client) // Connect
            runSingleProcess(server) // Process Handshake Request
            runSingleProcess(client) // Process Handshake Response

            val otherClientConnection = otherFutConnection.get
            val otherServerConnection = server.getConnections.head

            val count = 1 + Random.nextInt(16)
            val pingFrame = new PingFrame("data".getBytes)
            val frameEncodedLength = pingFrame.getEncodedLength(true)
            var x = 0
            while (x < count) {
                val sent = clientConnection.sendFrame(pingFrame)
                sent shouldBe true
                x += 1
            }

            clientConnection.getFramesWritten shouldBe 0
            clientConnection.getFrameBytesWritten shouldBe 0
            runSingleProcess(client)
            clientConnection.getFramesWritten shouldBe count
            clientConnection.getFrameBytesWritten shouldBe frameEncodedLength * count

            serverConnection.getFramesRead shouldBe 0
            serverConnection.getFrameBytesRead shouldBe 0
            runSingleProcess(server)
            serverConnection.getFramesRead shouldBe count
            serverConnection.getFrameBytesRead shouldBe frameEncodedLength * count
            serverConnection.getFramesHandled shouldBe count
        }
    }

    it should "fail writes once the write enqueue is full" in repeat(512) {
        withServerAndClient(noopFrameHandler, noopFrameHandler) { (server, client) =>
            val futConnection = client.connect(address)
            runSingleProcess(server) // Accept
            runSingleProcess(client) // Connect
            runSingleProcess(server) // Process Handshake Request
            runSingleProcess(client) // Process Handshake Response

            val clientConnection = futConnection.get
            val serverConnection = server.getConnections.head

            val writeQueueCapacity = clientConnection.getWriteQueueCapacity

            val pingFrame = new PingFrame("data".getBytes)

            var totalSent = 0
            while (totalSent < writeQueueCapacity) {
                val sent = clientConnection.sendFrame(pingFrame)
                sent shouldBe true
                totalSent += 1
            }

            // Now that the buffer is full, one more write should fail
            val sent = clientConnection.sendFrame(pingFrame)
            sent shouldBe false
        }
    }

    it should "eventually fail writes even while flushing due to the kernel buffer being full" in repeat(16) {
        withServerAndClient(noopFrameHandler, noopFrameHandler) { (server, client) =>
            val futConnection = client.connect(address)
            runSingleProcess(server) // Accept
            runSingleProcess(client) // Connect
            runSingleProcess(server) // Process Handshake Request
            runSingleProcess(client) // Process Handshake Response

            val clientConnection = futConnection.get
            val serverConnection = server.getConnections.head

            val writeQueueCapacity = clientConnection.getWriteQueueCapacity

            val pingFrame = new PingFrame("data".getBytes)
            val frameEncodedLength = pingFrame.getEncodedLength(true)

            var totalSent = 0
            var sent = clientConnection.sendFrame(pingFrame)
            while (sent) {
                totalSent += 1
                runProcess(client)
                sent = clientConnection.sendFrame(pingFrame)
            }
            println(s"Sent $totalSent before failure.")
        }
    }

    it should "write in bulk correctly" in repeat(16) {
        withServerAndClient(noopFrameHandler, noopFrameHandler) { (server, client) =>
            val futConnection = client.connect(address)
            runSingleProcess(server) // Accept
            runSingleProcess(client) // Connect
            runSingleProcess(server) // Process Handshake Request
            runSingleProcess(client) // Process Handshake Response

            val clientConnection = futConnection.get
            val serverConnection = server.getConnections.head

            val writeQueueCapacity = clientConnection.getWriteQueueCapacity

            val pingFrame = new PingFrame("data".getBytes)
            val frameEncodedLength = pingFrame.getEncodedLength(true)

            val count = 10000000

            val clientThread = new Thread {
                override def run(): Unit = {
                try {
                    while (clientConnection.getFramesWritten < count) {
                        clientConnection.inQueue.set(true)
                        clientConnection.process()
                        Thread.`yield`()
                    }
                    println(s"Client done sending")
                }
                catch {
                    case ex: ClosedByInterruptException =>
                }
            }
            }

            val serverThread = new Thread {
                override def run(): Unit = {
                try {
                    while (serverConnection.getFramesRead < count) {
                        //                            println(s"${serverConnection.getFrameBytesRead} / ${serverConnection.getFramesRead}")
                        serverConnection.inQueue.set(true)
                        serverConnection.process()
                        Thread.`yield`()
                    }
                }
                catch {
                    case ex: ClosedByInterruptException =>
                }
            }
            }

            val pre = System.currentTimeMillis()
            clientThread.start()
            serverThread.start()
            var totalSent = 0
            var failedSent = 0
            while (totalSent < count) {
                val sent = clientConnection.sendFrame(pingFrame)
                if (sent) {
                    totalSent += 1
                }
                else {
                    failedSent += 1
                    Thread.sleep(0, 1)
                }
            }

            println(s"Send done! $failedSent failed to send and had to be retried")

            clientThread.join()
            serverThread.join()

            clientConnection.getFramesWritten should equal(count)
            clientConnection.getFrameBytesWritten should equal(count * frameEncodedLength)
            serverConnection.getFrameBytesRead should equal(count * frameEncodedLength)
            serverConnection.getFramesRead should equal(count)

            val post = System.currentTimeMillis()

            println(s"Took ${post - pre} ms")
        }
    }

    it should "write in bulk correctly across multiple connections" in repeat(16) {
        val serverFrameHandler = new FrameHandler {
            val totalLatency = new LongAdder()
            override def handleCloseFrame(frame: CloseFrame, connection: HttpConnection): Unit = ()
            override def handlePongFrame(frame: PongFrame, connection: HttpConnection): Unit = ()
            override def handleBinaryFrame(frame: BinaryFrame, connection: HttpConnection): Unit = ()
            override def handleTextFrame(frame: TextFrame, connection: HttpConnection): Unit = ()
            override def handlePingFrame(frame: PingFrame, connection: HttpConnection): Unit = {
            val now = System.currentTimeMillis()
            val sentTime = ByteBuffer.wrap(frame.getPayload).getLong
            totalLatency.addAsync(now - sentTime)
        }
        }
        withServerAndClient(serverFrameHandler, noopFrameHandler) { (server, client) =>
            val connectionCount = 400

            println(s"Starting $connectionCount connections")
            val connectStart = System.currentTimeMillis()
            var c = 0
            while(c < connectionCount){
                client.connect(address)
                runProcess(server)
                runProcess(client)
                c += 1
            }

            eventually {
                runProcess(server)
                runProcess(client)
                server.connections.size should equal(connectionCount)
                client.connections.size should equal(connectionCount)
                Thread.`yield`()
            }

            val connectEnd = System.currentTimeMillis()

            println(s"All connections accepted, took ${connectEnd - connectStart} ms")

            val clientConnections = client.getConnections.toList
            val serverConnections = server.getConnections.toList

            val timeBytes = new Array[Byte](8)
            val exampleFrame = new PingFrame(timeBytes)
            val frameEncodedLength = exampleFrame.getEncodedLength(true)

            val count = 100L

            val finishedClientConnections = new ConcurrentHashMap[SelectableHandler, Boolean]()

            val clientThreads = for {
                c <- 0 until 2
                thread = new Thread("ClientThread") {
                    override def run(): Unit = {
                    try {
                        val enqueue = client.getProcessQueue
                        var loopCount = 0L

                        while (finishedClientConnections.size() < connectionCount) {
                            loopCount += 1
                            val handler = enqueue.poll(10, TimeUnit.MILLISECONDS)
                            if (handler != null) {
                                handler.process()
                                if(handler.asInstanceOf[HttpClientConnection].getFrameBytesWritten == count * frameEncodedLength){
                                    finishedClientConnections.put(handler, true)
                                }
                            }
                        }
                        println(s"Client done sending : ${clientConnections.map(_.getFramesWritten).sum}")
                    }
                    catch {
                        case ex: ClosedByInterruptException =>
                    }
                }
                }
            } yield thread

            val finishedServerConnections = new ConcurrentHashMap[SelectableHandler, Boolean]()

            val serverThreads = for {
                c <- 0 until 4
                thread = new Thread("ServerThread") {
                    override def run(): Unit = {
                    try {
                        var loopCount = 0
                        val enqueue = server.getProcessQueue
                        while (finishedServerConnections.size() < connectionCount) {
                            loopCount += 1

                            val handler = enqueue.poll(10, TimeUnit.MILLISECONDS)
                            if (handler != null) {
                                handler.process()
                                if(handler.asInstanceOf[HttpServerConnection].getFramesHandled == count){
                                    finishedServerConnections.put(handler, true)
                                }
                            }
                        }
                        println(s"Server thread finished, loopCount: $loopCount, expected ${count * connectionCount}")
                    }
                    catch {
                        case ex: ClosedByInterruptException =>
                    }
                }
                }
            } yield thread

            val pre = System.currentTimeMillis()

            val sendThreadCount = 4
            val sendThreads = for {
                c <- 0 until sendThreadCount
                thread = new Thread(s"SendThread-$c") {
                    var totalSent = 0
                    var failedSent = 0
                    override def run(): Unit = {
                    while (totalSent < count * (connectionCount / sendThreadCount)) {
                        val timeBytes = new Array[Byte](8)
                        ByteBuffer.wrap(timeBytes).putLong(0, System.currentTimeMillis())
                        val pingFrame = new PingFrame(timeBytes)
                        val sent = clientConnections(totalSent % connectionCount).sendFrame(pingFrame)
                        if (sent) {
                            totalSent += 1
                        }
                        else {
                            failedSent += 1
                            Thread.sleep(0, 1)
                        }
                    }
                    println(s"Send thread done, sent $totalSent, $failedSent failed to send and had to be retried")
                }
                }
            } yield thread

            clientThreads.foreach(_.start())
            serverThreads.foreach(_.start())
            sendThreads.foreach(_.start())
            sendThreads.foreach(_.join())

            val sendEnd = System.currentTimeMillis()
            println(s"Send done! Send took ${sendEnd - pre} ms")

            clientThreads.foreach(_.join())

            clientConnections.foreach { clientConnection =>
                clientConnection.getFramesWritten should equal(count)
                clientConnection.getFrameBytesWritten should equal(count * frameEncodedLength)
            }

            serverThreads.foreach(_.join())

            serverConnections.foreach { serverConnection =>
//                println(serverConnection.getFramesRead)
                serverConnection.getFrameBytesRead should equal(count * frameEncodedLength)
                serverConnection.getFramesRead should equal(count)
            }

            val post = System.currentTimeMillis()

            //            println(s"Total spent in client : ${clientConnection.totalTimeTaken}")
            //            println(s"Total spent in server : ${serverConnection.totalTimeTaken}")
            println(s"Took ${post - pre} ms")
            println(s"Average latency : ${serverFrameHandler.totalLatency.sum / (count * connectionCount)} ms")
        }
    }*/


    //
    //    "LongAdder" should "handle high contention well" in repeat(16) {
    //        val adder = new LongAdder()
    //
    //        val count = 100000000
    //        var x = 0
    //
    //        val threadCount = 8
    //
    //        val threads = for{
    //            t <- 1 to threadCount
    //            thread = new Thread {
    //                override def run(): Unit = {
    //                    var x = 0
    //                    while(x < count / threadCount){
    //                        adder.increment()
    //                        x += 1
    //                        if(x % 10 == 0){
    //                            var y = 0
    //                            while(y < 1) {
    //                                // TODO: WHAT!? this loop vs without the loop is significantly faster, WTF?
    //                                adder.sum()
    //                                y += 1
    //                            }
    //                        }
    //                    }
    //                }
    //            }
    //        } yield thread
    //
    //        val pre = System.currentTimeMillis()
    //        threads.foreach(_.start)
    //        threads.foreach(_.join)
    //        val post = System.currentTimeMillis()
    //        println(s"Took ${post - pre}")
    ////        if(adder.sum < count){
    ////            ???
    ////        }
    //        adder.sum shouldBe count
    //    }
    //
    //    "Selector" should "select" in {
    //        val selector = Selector.open()
    //
    //        val serverSocket = ServerSocketChannel.open()
    //        serverSocket.bind(address)
    //
    //        val clientSocket = SocketChannel.open()
    //        clientSocket.configureBlocking(false)
    //        clientSocket.connect(address)
    //
    //        val acceptedSocket = serverSocket.accept()
    //        acceptedSocket.configureBlocking(false)
    //
    //        clientSocket.finishConnect()
    //
    //        val selectionKey = clientSocket.register(selector, SelectionKey.OP_READ)
    //        acceptedSocket.write(ByteBuffer.wrap("data".getBytes))
    //        selector.select()
    //        println(selector.selectedKeys.size)
    //        println(selectionKey.readyOps())
    //        selectionKey.interestOps(0)
    //        println(selectionKey.readyOps() & SelectionKey.OP_READ)
    //
    //    }
}
*/
*/
