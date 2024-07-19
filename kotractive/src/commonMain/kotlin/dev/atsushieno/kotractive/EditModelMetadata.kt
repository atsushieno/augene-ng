package dev.atsushieno.kotractive

enum class DataType
{
    Unknown,
    String,
    UnixTime,
    Id,
    Length,
    Number,
    Integer,
    BooleanInt,
    Color,
    HexBinary,
    Base64Binary
}

class ControlType
{
    companion object {
        const val ProgramChange = 0x1000 + 1
        const val PAf = 0x1000 + 4
        const val PitchBend = 0x1000 + 5
        const val CAf = 0x1000 + 7
    }
}

@Target(AnnotationTarget.FIELD)
annotation class DataTypes(val dataType: DataType)


// LAMESPEC: this use of expect/actual is weird and stupid, but with KSP on MPP this seems to be the only way to
//  generate compilable code.
//expect fun initializeModelCatalog()

val typeBoolean = MetaType("Boolean", "kotlin.Boolean", null, TypeCode.Boolean)
val typeInt = MetaType("Int", "kotlin.Int", null, TypeCode.Int32)
val typeLong = MetaType("Long", "kotlin.Long", null, TypeCode.Int64)
val typeDouble = MetaType("Double", "kotlin.Double", null, TypeCode.Double)
val typeString = MetaType("String", "kotlin.String", null, TypeCode.String)
val typeMutableList = MetaType("MutableList", "kotlin.collections.MutableList", null, TypeCode.Object)
val typeArray = MetaType("Array", "kotlin.Array", null, TypeCode.Object)

object ModelCatalog {
    val allTypes = mutableListOf<MetaType>()

    init {
        allTypes.add(typeBoolean)
        allTypes.add(typeInt)
        allTypes.add(typeLong)
        allTypes.add(typeDouble)
        allTypes.add(typeString)
        allTypes.add(typeMutableList)
        allTypes.add(typeArray)
        initializeModelCatalog()
    }
}

enum class TypeCode {
    Object,
    Boolean,
    Double,
    Int32,
    Int64,
    String,
}

// System.Type in the original .NET implementation. Omitted a lot of unused bits.
open class MetaType(val simpleName: String, val qualifiedName: String, val baseMetaType: MetaType?, val typeCode: TypeCode = TypeCode.Object) {

    override fun toString() = qualifiedName

    open fun newInstance() : Any =
        throw IllegalArgumentException("getAddMethod is available only on EditModel element types.")

    val declaredProperties = mutableListOf<PropertyInfo>()

    val properties : Iterable<PropertyInfo>
        get() = declaredProperties + (baseMetaType?.properties ?: listOf())

    fun getProperty(name: String) = properties.firstOrNull { p -> p.name == name }

    fun isAssignableFrom(other: MetaType) : Boolean {
        if (qualifiedName == other.qualifiedName)
            return true
        if (other.baseMetaType == null)
            return false
        return isAssignableFrom(other.baseMetaType)
    }
}

fun Any.getMetaType() : MetaType =
    ModelCatalog.allTypes.first { this::class.simpleName == it.simpleName } // Kotlin/JS does not support qualifiedName (yet?)

abstract class PropertyInfo(val name: String, val propertyMetaType: MetaType, val dataType: DataType) {
    abstract val ownerType: MetaType
    abstract fun getValue(target: Any) : Any?
    open fun setValue(target: Any, value: Any?) {
        throw IllegalStateException("This property does not support setter.")
    }
    open val listItemType : MetaType
        get() = throw IllegalArgumentException("listItemType is available only on list type, not on ${ownerType.qualifiedName}.$name")

    open fun addListItem(target: Any, itemObj: Any?) {
        throw IllegalArgumentException("getAddMethod is available only on list type.")
    }
}
