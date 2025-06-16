@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package com.jetbrains.rider.azure.model

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.impl.*

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.*
import com.jetbrains.rd.util.*
import kotlin.time.Duration
import kotlin.reflect.KClass
import kotlin.jvm.JvmStatic



/**
 * #### Generated from [FunctionAppDaemonModel.kt:14]
 */
class FunctionAppDaemonModel private constructor(
    private val _runFunctionApp: RdSignal<FunctionAppRequest>,
    private val _debugFunctionApp: RdSignal<FunctionAppRequest>,
    private val _triggerFunctionApp: RdSignal<FunctionAppTriggerRequest>,
    private val _getAzureFunctionsVersion: RdCall<AzureFunctionsVersionRequest, String?>,
    private val _getAzureFunctionWorkerModel: RdCall<AzureFunctionWorkerModelRequest, AzureFunctionWorkerModel>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            val classLoader = javaClass.classLoader
            serializers.register(LazyCompanionMarshaller(RdId(4352787550093553180), classLoader, "com.jetbrains.rider.azure.model.FunctionAppTriggerType"))
            serializers.register(LazyCompanionMarshaller(RdId(5924805963354225314), classLoader, "com.jetbrains.rider.azure.model.FunctionAppHttpTriggerAttribute"))
            serializers.register(LazyCompanionMarshaller(RdId(-6716933343451164563), classLoader, "com.jetbrains.rider.azure.model.FunctionAppTriggerRequest"))
            serializers.register(LazyCompanionMarshaller(RdId(4671209338920827033), classLoader, "com.jetbrains.rider.azure.model.FunctionAppRequest"))
            serializers.register(LazyCompanionMarshaller(RdId(2602283917799194934), classLoader, "com.jetbrains.rider.azure.model.AzureFunctionsVersionRequest"))
            serializers.register(LazyCompanionMarshaller(RdId(4069196274151977752), classLoader, "com.jetbrains.rider.azure.model.AzureFunctionWorkerModelRequest"))
            serializers.register(LazyCompanionMarshaller(RdId(7072279419286197111), classLoader, "com.jetbrains.rider.azure.model.AzureFunctionWorkerModel"))
        }
        
        
        
        
        private val __StringNullableSerializer = FrameworkMarshallers.String.nullable()
        
        const val serializationHash = -6648751372103047614L
        
    }
    override val serializersOwner: ISerializersOwner get() = FunctionAppDaemonModel
    override val serializationHash: Long get() = FunctionAppDaemonModel.serializationHash
    
    //fields
    
    /**
     * Signal from backend to run a Function App locally.
     */
    val runFunctionApp: ISource<FunctionAppRequest> get() = _runFunctionApp
    
    /**
     * Signal from backend to debug a Function App locally.
     */
    val debugFunctionApp: ISource<FunctionAppRequest> get() = _debugFunctionApp
    
    /**
     * Signal from backend to trigger a Function App.
     */
    val triggerFunctionApp: ISource<FunctionAppTriggerRequest> get() = _triggerFunctionApp
    
    /**
     * Request from frontend to read the AzureFunctionsVersion MSBuild property.
     */
    val getAzureFunctionsVersion: IRdCall<AzureFunctionsVersionRequest, String?> get() = _getAzureFunctionsVersion
    
    /**
     * Request from fronted to guess the function worker model by installed packages.
     */
    val getAzureFunctionWorkerModel: IRdCall<AzureFunctionWorkerModelRequest, AzureFunctionWorkerModel> get() = _getAzureFunctionWorkerModel
    //methods
    //initializer
    init {
        bindableChildren.add("runFunctionApp" to _runFunctionApp)
        bindableChildren.add("debugFunctionApp" to _debugFunctionApp)
        bindableChildren.add("triggerFunctionApp" to _triggerFunctionApp)
        bindableChildren.add("getAzureFunctionsVersion" to _getAzureFunctionsVersion)
        bindableChildren.add("getAzureFunctionWorkerModel" to _getAzureFunctionWorkerModel)
    }
    
    //secondary constructor
    internal constructor(
    ) : this(
        RdSignal<FunctionAppRequest>(FunctionAppRequest),
        RdSignal<FunctionAppRequest>(FunctionAppRequest),
        RdSignal<FunctionAppTriggerRequest>(FunctionAppTriggerRequest),
        RdCall<AzureFunctionsVersionRequest, String?>(AzureFunctionsVersionRequest, __StringNullableSerializer),
        RdCall<AzureFunctionWorkerModelRequest, AzureFunctionWorkerModel>(AzureFunctionWorkerModelRequest, AzureFunctionWorkerModel.marshaller)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("FunctionAppDaemonModel (")
        printer.indent {
            print("runFunctionApp = "); _runFunctionApp.print(printer); println()
            print("debugFunctionApp = "); _debugFunctionApp.print(printer); println()
            print("triggerFunctionApp = "); _triggerFunctionApp.print(printer); println()
            print("getAzureFunctionsVersion = "); _getAzureFunctionsVersion.print(printer); println()
            print("getAzureFunctionWorkerModel = "); _getAzureFunctionWorkerModel.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): FunctionAppDaemonModel   {
        return FunctionAppDaemonModel(
            _runFunctionApp.deepClonePolymorphic(),
            _debugFunctionApp.deepClonePolymorphic(),
            _triggerFunctionApp.deepClonePolymorphic(),
            _getAzureFunctionsVersion.deepClonePolymorphic(),
            _getAzureFunctionWorkerModel.deepClonePolymorphic()
        )
    }
    //contexts
    //threading
    override val extThreading: ExtThreadingKind get() = ExtThreadingKind.Default
}
val com.jetbrains.rd.ide.model.Solution.functionAppDaemonModel get() = getOrCreateExtension("functionAppDaemonModel", ::FunctionAppDaemonModel)



/**
 * #### Generated from [FunctionAppDaemonModel.kt:49]
 */
enum class AzureFunctionWorkerModel {
    Default, 
    Isolated, 
    Unknown;
    
    companion object : IMarshaller<AzureFunctionWorkerModel> {
        val marshaller = FrameworkMarshallers.enum<AzureFunctionWorkerModel>()
        
        
        override val _type: KClass<AzureFunctionWorkerModel> = AzureFunctionWorkerModel::class
        override val id: RdId get() = RdId(7072279419286197111)
        
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): AzureFunctionWorkerModel {
            return marshaller.read(ctx, buffer)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: AzureFunctionWorkerModel)  {
            marshaller.write(ctx, buffer, value)
        }
    }
}


/**
 * #### Generated from [FunctionAppDaemonModel.kt:45]
 */
data class AzureFunctionWorkerModelRequest (
    val projectFilePath: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<AzureFunctionWorkerModelRequest> {
        override val _type: KClass<AzureFunctionWorkerModelRequest> = AzureFunctionWorkerModelRequest::class
        override val id: RdId get() = RdId(4069196274151977752)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): AzureFunctionWorkerModelRequest  {
            val projectFilePath = buffer.readString()
            return AzureFunctionWorkerModelRequest(projectFilePath)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: AzureFunctionWorkerModelRequest)  {
            buffer.writeString(value.projectFilePath)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as AzureFunctionWorkerModelRequest
        
        if (projectFilePath != other.projectFilePath) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + projectFilePath.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("AzureFunctionWorkerModelRequest (")
        printer.indent {
            print("projectFilePath = "); projectFilePath.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [FunctionAppDaemonModel.kt:41]
 */
data class AzureFunctionsVersionRequest (
    val projectFilePath: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<AzureFunctionsVersionRequest> {
        override val _type: KClass<AzureFunctionsVersionRequest> = AzureFunctionsVersionRequest::class
        override val id: RdId get() = RdId(2602283917799194934)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): AzureFunctionsVersionRequest  {
            val projectFilePath = buffer.readString()
            return AzureFunctionsVersionRequest(projectFilePath)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: AzureFunctionsVersionRequest)  {
            buffer.writeString(value.projectFilePath)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as AzureFunctionsVersionRequest
        
        if (projectFilePath != other.projectFilePath) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + projectFilePath.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("AzureFunctionsVersionRequest (")
        printer.indent {
            print("projectFilePath = "); projectFilePath.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [FunctionAppDaemonModel.kt:20]
 */
data class FunctionAppHttpTriggerAttribute (
    val authLevel: String?,
    val methods: List<String?>,
    val route: String?,
    val routeForHttpClient: String?
) : IPrintable {
    //companion
    
    companion object : IMarshaller<FunctionAppHttpTriggerAttribute> {
        override val _type: KClass<FunctionAppHttpTriggerAttribute> = FunctionAppHttpTriggerAttribute::class
        override val id: RdId get() = RdId(5924805963354225314)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): FunctionAppHttpTriggerAttribute  {
            val authLevel = buffer.readNullable { buffer.readString() }
            val methods = buffer.readList { buffer.readNullable { buffer.readString() } }
            val route = buffer.readNullable { buffer.readString() }
            val routeForHttpClient = buffer.readNullable { buffer.readString() }
            return FunctionAppHttpTriggerAttribute(authLevel, methods, route, routeForHttpClient)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: FunctionAppHttpTriggerAttribute)  {
            buffer.writeNullable(value.authLevel) { buffer.writeString(it) }
            buffer.writeList(value.methods) { v -> buffer.writeNullable(v) { buffer.writeString(it) } }
            buffer.writeNullable(value.route) { buffer.writeString(it) }
            buffer.writeNullable(value.routeForHttpClient) { buffer.writeString(it) }
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as FunctionAppHttpTriggerAttribute
        
        if (authLevel != other.authLevel) return false
        if (methods != other.methods) return false
        if (route != other.route) return false
        if (routeForHttpClient != other.routeForHttpClient) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + if (authLevel != null) authLevel.hashCode() else 0
        __r = __r*31 + methods.hashCode()
        __r = __r*31 + if (route != null) route.hashCode() else 0
        __r = __r*31 + if (routeForHttpClient != null) routeForHttpClient.hashCode() else 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("FunctionAppHttpTriggerAttribute (")
        printer.indent {
            print("authLevel = "); authLevel.print(printer); println()
            print("methods = "); methods.print(printer); println()
            print("route = "); route.print(printer); println()
            print("routeForHttpClient = "); routeForHttpClient.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [FunctionAppDaemonModel.kt:35]
 */
data class FunctionAppRequest (
    val methodName: String?,
    val functionName: String?,
    val projectFilePath: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<FunctionAppRequest> {
        override val _type: KClass<FunctionAppRequest> = FunctionAppRequest::class
        override val id: RdId get() = RdId(4671209338920827033)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): FunctionAppRequest  {
            val methodName = buffer.readNullable { buffer.readString() }
            val functionName = buffer.readNullable { buffer.readString() }
            val projectFilePath = buffer.readString()
            return FunctionAppRequest(methodName, functionName, projectFilePath)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: FunctionAppRequest)  {
            buffer.writeNullable(value.methodName) { buffer.writeString(it) }
            buffer.writeNullable(value.functionName) { buffer.writeString(it) }
            buffer.writeString(value.projectFilePath)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as FunctionAppRequest
        
        if (methodName != other.methodName) return false
        if (functionName != other.functionName) return false
        if (projectFilePath != other.projectFilePath) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + if (methodName != null) methodName.hashCode() else 0
        __r = __r*31 + if (functionName != null) functionName.hashCode() else 0
        __r = __r*31 + projectFilePath.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("FunctionAppRequest (")
        printer.indent {
            print("methodName = "); methodName.print(printer); println()
            print("functionName = "); functionName.print(printer); println()
            print("projectFilePath = "); projectFilePath.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [FunctionAppDaemonModel.kt:27]
 */
data class FunctionAppTriggerRequest (
    val methodName: String,
    val functionName: String,
    val triggerType: FunctionAppTriggerType,
    val httpTriggerAttribute: FunctionAppHttpTriggerAttribute?,
    val projectFilePath: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<FunctionAppTriggerRequest> {
        override val _type: KClass<FunctionAppTriggerRequest> = FunctionAppTriggerRequest::class
        override val id: RdId get() = RdId(-6716933343451164563)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): FunctionAppTriggerRequest  {
            val methodName = buffer.readString()
            val functionName = buffer.readString()
            val triggerType = buffer.readEnum<FunctionAppTriggerType>()
            val httpTriggerAttribute = buffer.readNullable { FunctionAppHttpTriggerAttribute.read(ctx, buffer) }
            val projectFilePath = buffer.readString()
            return FunctionAppTriggerRequest(methodName, functionName, triggerType, httpTriggerAttribute, projectFilePath)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: FunctionAppTriggerRequest)  {
            buffer.writeString(value.methodName)
            buffer.writeString(value.functionName)
            buffer.writeEnum(value.triggerType)
            buffer.writeNullable(value.httpTriggerAttribute) { FunctionAppHttpTriggerAttribute.write(ctx, buffer, it) }
            buffer.writeString(value.projectFilePath)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as FunctionAppTriggerRequest
        
        if (methodName != other.methodName) return false
        if (functionName != other.functionName) return false
        if (triggerType != other.triggerType) return false
        if (httpTriggerAttribute != other.httpTriggerAttribute) return false
        if (projectFilePath != other.projectFilePath) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + methodName.hashCode()
        __r = __r*31 + functionName.hashCode()
        __r = __r*31 + triggerType.hashCode()
        __r = __r*31 + if (httpTriggerAttribute != null) httpTriggerAttribute.hashCode() else 0
        __r = __r*31 + projectFilePath.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("FunctionAppTriggerRequest (")
        printer.indent {
            print("methodName = "); methodName.print(printer); println()
            print("functionName = "); functionName.print(printer); println()
            print("triggerType = "); triggerType.print(printer); println()
            print("httpTriggerAttribute = "); httpTriggerAttribute.print(printer); println()
            print("projectFilePath = "); projectFilePath.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [FunctionAppDaemonModel.kt:15]
 */
enum class FunctionAppTriggerType {
    HttpTrigger, 
    Other;
    
    companion object : IMarshaller<FunctionAppTriggerType> {
        val marshaller = FrameworkMarshallers.enum<FunctionAppTriggerType>()
        
        
        override val _type: KClass<FunctionAppTriggerType> = FunctionAppTriggerType::class
        override val id: RdId get() = RdId(4352787550093553180)
        
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): FunctionAppTriggerType {
            return marshaller.read(ctx, buffer)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: FunctionAppTriggerType)  {
            marshaller.write(ctx, buffer, value)
        }
    }
}
