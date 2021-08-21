package dev.atsushieno.kotractive_ksp

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.validate
import java.io.Writer

class KotractiveSymbolProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private val codeGenerator = environment.codeGenerator
    private val logger = environment.logger

    private val visitor = KotractiveVisitor(this)

    private val allTypes = mutableListOf<KSClassDeclaration>()
    private val elements = mutableListOf<KSName>()

    private var iteration = 0

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (resolver.getAllFiles().any { it.filePath.contains("commonTest") })
            return listOf()
        iteration++

        val actualFiles = resolver.getNewFiles().filter { f -> !f.filePath.contains("build/generated/ksp" )}.toList()

        // It should run only against non-generated non-test code.
        if (actualFiles.isEmpty())
            return listOf()

        // process EditModel and generate all the relevant MetaType code.
        val editModelFile = actualFiles.first { it.fileName == "EditModel.kt" }
        editModelFile.accept(visitor, Unit)

        val writer = codeGenerator.createNewFile(Dependencies(false), "dev.atsushieno.kotractive", "EditModelMetadataGenerated").writer()
        writer.write("package dev.atsushieno.kotractive\n")

        writer.write("""
actual fun initializeModelCatalog() {
""")
        elements.forEach {
            writer.write("""    ModelCatalog.allTypes.add(type${it.getShortName()})
""")
        }
        writer.write("""
}
""")

        writer.close()

        return listOf()
    }

    fun registerType(classDeclaration: KSClassDeclaration) {
        // Skip enum types (such as dev.atsushieno.kotractive.DataType)
        if (classDeclaration.superTypes.map { st -> st.resolve() }.filterIsInstance<KSClassDeclaration>()
                .any { skd -> skd.simpleName.asString() == "Enum" })
            return

        val fullName = classDeclaration.qualifiedName?.asString() ?: return
        if (allTypes.any { fullName == it.qualifiedName?.asString() })
            return
        allTypes.add(classDeclaration)

        val name = classDeclaration.qualifiedName?.getShortName()
        val baseType = classDeclaration.superTypes.firstOrNull()?.resolve()?.declaration
        val baseTypeSpec = if (baseType == null) "null" else "type${baseType.qualifiedName!!.getShortName()}"

        val writer = codeGenerator.createNewFile(Dependencies(false), "dev.atsushieno.kotractive", "MetaType${name}").writer()
        writer.write("""
package dev.atsushieno.kotractive

internal class MetaType$name : MetaType("$name", "$fullName", $baseTypeSpec) {
""")
        if (fullName.endsWith("Element") || fullName.endsWith("ElementBase") || fullName.endsWith("ContentBase")) {
            elements.add(classDeclaration.qualifiedName!!)
            if (!classDeclaration.isAbstract())
                writer.write("    override fun newInstance() =  $name()\n")
            else
                writer.write("    override fun newInstance() = IllegalStateException()\n")
        }

        // Register declaredProperties at init{}
        writer.write("    init {")
        classDeclaration.declarations.filterIsInstance<KSPropertyDeclaration>().forEach { property ->
            val propertyName = property.simpleName.asString()
            val propertyTypeDecl = property.type.resolve()
            if (propertyTypeDecl is KSClassDeclaration)
                registerType(propertyTypeDecl)

            // interpret @DataType annotation on each property
            val dataTypeAnnotation = property.annotations.firstOrNull { it.annotationType.toString() == "DataTypes" }
            val dataTypeSpec = if (dataTypeAnnotation != null) dataTypeAnnotation.arguments.first().value!!.toString() else "Unknown"

            val optNullableAssignment = if (propertyTypeDecl.nullability == Nullability.NULLABLE) "if (value == null) null else" else ""
            val propertyTypeGenericArgs = if (propertyTypeDecl.arguments.any()) "<${propertyTypeDecl.arguments.map { a -> a.type.toString() }.joinToString(", ")}>" else ""
            writer.write("""
        declaredProperties.add(object: PropertyInfo("$propertyName", type${property.type}, DataType.$dataTypeSpec) {
            override val ownerType
                get() = type$name
            override fun toString() = "${property.qualifiedName!!.asString()}"
            override fun getValue(target: Any) = (target as ${classDeclaration.simpleName.asString()}).$propertyName
""")
            if (property.isMutable)
                writer.write("""        
            override fun setValue(target: Any, value: Any?) {
                (target as ${classDeclaration.simpleName.asString()}).$propertyName = $optNullableAssignment value as ${property.type}$propertyTypeGenericArgs
            }
""")
            // add `addLustItem` override if property type is MutableList
            if (property.type.toString().contains("MutableList")) {
                val listItemType = propertyTypeDecl.arguments.first().type
                writer.write("""
            override val listItemType
                get() = type${listItemType}
            override fun addListItem(target: Any, value: Any?) {
                (target as MutableList${propertyTypeGenericArgs}).add(value as $listItemType)
            }
""")
            }
            writer.write("""
        })
""")
        }
        writer.write("""    }
}
internal val type$name = MetaType$name()
""")

        writer.close()
    }

    inner class KotractiveVisitor(private val owner: KotractiveSymbolProcessor) : KSVisitorVoid() {
        override fun visitFile(file: KSFile, data: Unit) {
            file.declarations.forEach {
                if (it.validate()) // seealso https://github.com/google/ksp/issues/574
                    it.accept(this, Unit)
            }
        }

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            owner.registerType(classDeclaration)
        }
    }
}