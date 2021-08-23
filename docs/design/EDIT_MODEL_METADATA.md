# EditModelMetadata: under the hood

augene-ng is originally written in C#, in a repository called [https://github.com/atsushieno/ntracktive](ntracktive), and its `EditModel` class made full use of .NET Reflection API as well as XML API.

The actual use of XML API involves XmlReader, XmlWriter, XmlSerializer and XLinq, and apart from XmlSerializer they were not hard to reimplement in Kotlin. And we replaced use of XmlSerializer with only tens of lines of code with XmlReader and XmlWriter. The XML implementation is now split into [missing-dot](https://github.com/atsushieno/missing-dot) project, for possible future API addition.

But the reflection part was not easily doable. There is no runtime type information in Kotlin Multiplatform, like, there is no `Type.GetProperties()`.

In Kotlin world, meta programming is typically done with code generation framework. In augene-ng we end up using [google/ksp](https://github.com/google/ksp/) that aims solid integration with Kotlin Compiler Plugin framework as well as solid Gradle integration. Our code generator is implemented in a separate module `kotractive_ksp`, and it is then used by `kotractive` module.

There are some non-dynamic helper classes (you can take them as "runtime") that are referenced by the generated code by kotractive_ksp, like `MetaType` classes  (which corresponds to `System.Type`) for each involved EditModel constructs (as well as basic types). It is not a comprehensive clone of .NET API; we drop a lot of complicated parts as well as giving a lot of non-universal assumptions (e.g. collections are always `MutableList<T>`). It may be possible to make them more general, but so far I (@atsushieno) do not plan to do it.

ksp also brought in some minor drawback - we used to be able to include multiple Multiplatform modules (which is not really "supported" by JetBrains anyways), but after adding ksp module it broke the build by some unexpected structural  / build property mutation. Thus I ended up to split the projects into three parts (kotractive / augene / augene-gui).

There was another minor (but very significant) pitfall with ksp - it works with Kotlin MPP in general, but not with Kotlin/Native as expected It involves some metadata compilation that fails. It is a significant blocker if we want to use "libaugene" **within** augene-player (we do).
