package java.nio.file

class Path {
    companion object {
        fun of(path: String) = Paths.get(path)
    }
}