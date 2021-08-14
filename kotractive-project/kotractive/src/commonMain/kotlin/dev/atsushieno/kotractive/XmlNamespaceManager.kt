package dev.atsushieno.kotractive

class XmlNamespaceManager {
	companion object {
		const val Xmlns2000 = "http://www.w3.org/2000/xmlns/"
	}

	private val scopes = mutableListOf<HashMap<String,String>>(HashMap())

	val defaultNamespace: String
		get() = lookupNamespace("") ?: ""

	fun addNamespace(prefix: String, ns: String) {
		scopes.last()[prefix] = ns
	}

	fun lookupNamespace(prefix: String) : String? {
		return scopes.lastOrNull { m -> m.containsKey(prefix) }?.get(prefix)
	}

	fun lookupPrefix(ns: String) : String? {
		return scopes.lastOrNull { m -> m.containsValue(ns) }?.entries?.first { p -> p.value == ns }?.key
	}

	fun pushScope() {
		scopes.add(HashMap())
	}

	fun popScope() {
		if (scopes.size == 1)
			throw IllegalStateException("Attempt to pop scope at its empty state")
		scopes.removeAt(scopes.size - 1)
	}

	fun removeNamespace(prefix: String, ns: String) {
		scopes.last().remove(prefix)
	}

	internal fun clearInScopeNamespaces() {
		scopes.last().clear()
	}

	init {
		addNamespace("", "")
	}
}