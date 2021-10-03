package dev.atsushieno.augene

fun main(args: Array<String>) {
    if (args.size == 0) {
        println("usage: augene-console [.augene project filename]")
        return
    }
    val model = AugeneCompiler()
    model.loadProjectFile(args[0])
    model.compile()
}
