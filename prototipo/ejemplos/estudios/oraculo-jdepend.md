# Oráculo externo: el grafo del mecanismo frente al de JDepend

JDepend 2.9.5 (guru.nidi) se ejecuta sobre los mismos binarios. Se comparan solo los paquetes del sistema (bajo la raíz y fuera de las exclusiones del catálogo) y las dependencias entre ellos. Los componentes cíclicos se calculan con el mismo algoritmo sobre ambos grafos, sin las exenciones, para que la comparación aísle la extracción.

| Sistema | Aristas mecanismo | Aristas JDepend | Comunes | Solo mecanismo | Solo JDepend | Paquetes en ciclo (mec./JD) | Ce igual |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Apache Commons Lang 3.14.0 | 43 | 43 | 43 | 0 | 0 | 12 / 12 | 18 de 18 |
| Jackson Databind 2.17.2 | 166 | 163 | 163 | 3 | 0 | 21 / 21 | 22 de 23 |
| ArchUnit 1.3.0 | 107 | 105 | 105 | 2 | 0 | 12 / 10 | 20 de 22 |
| Guava 33.2.1 | 63 | 48 | 48 | 15 | 0 | 0 / 0 | 3 de 18 |
| Spring Framework PetClinic | 13 | 13 | 13 | 0 | 0 | 0 / 0 | 9 de 9 |
| MyBatis JPetStore 6 | 5 | 5 | 5 | 0 | 0 | 0 / 0 | 5 de 5 |
| DDD Sample (Citerus) | 93 | 94 | 93 | 0 | 1 | 5 / 5 | 25 de 26 |
| Caso de estudio (C0) | 5 | 5 | 5 | 0 | 0 | 0 / 0 | 4 de 4 |

# Diferencias, arista por arista

## Jackson Databind 2.17.2

- solo mecanismo: `com.fasterxml.jackson.databind.annotation -> com.fasterxml.jackson.databind.deser` — [Method <com.fasterxml.jackson.databind.annotation.JsonValueInstantiator.value()> has generic return type <java.lang.Class<? extends com.fasterxml.jackson.databind.deser.ValueInstantiator>> with type argument depending on <com.fasterxml.jackson.databind.deser.ValueInstantiator> in (JsonValueInstantiator.java:0)]
- solo mecanismo: `com.fasterxml.jackson.databind.annotation -> com.fasterxml.jackson.databind.jsontype` — [Method <com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver.value()> has generic return type <java.lang.Class<? extends com.fasterxml.jackson.databind.jsontype.TypeIdResolver>> with type argument depending on <com.fasterxml.jackson.databind.jsontype.TypeIdResolver> in (JsonTypeIdResolver.java:0), Method <com.fasterxml.jackson.databind.annotation.JsonTypeResolver.value()> has generic return type <java.lang.Class<? extends com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder<?>>> with type argument depending on <com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder> in (JsonTypeResolver.java:0)]
- solo mecanismo: `com.fasterxml.jackson.databind.annotation -> com.fasterxml.jackson.databind.ser` — [Method <com.fasterxml.jackson.databind.annotation.JsonAppend$Prop.value()> has generic return type <java.lang.Class<? extends com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter>> with type argument depending on <com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter> in (JsonAppend.java:0)]

## ArchUnit 1.3.0

- solo mecanismo: `com.tngtech.archunit -> com.tngtech.archunit.core.importer.resolvers` — [Method <com.tngtech.archunit.ArchConfiguration.setClassResolver(java.lang.Class)> has generic parameter type <java.lang.Class<? extends com.tngtech.archunit.core.importer.resolvers.ClassResolver>> with type argument depending on <com.tngtech.archunit.core.importer.resolvers.ClassResolver> in (ArchConfiguration.java:0)]
- solo mecanismo: `com.tngtech.archunit.core.importer.resolvers -> com.tngtech.archunit.core.domain` — [Method <com.tngtech.archunit.core.importer.resolvers.ClassResolver.tryResolve(java.lang.String)> has generic return type <java.util.Optional<com.tngtech.archunit.core.domain.JavaClass>> with type argument depending on <com.tngtech.archunit.core.domain.JavaClass> in (ClassResolver.java:0), Method <com.tngtech.archunit.core.importer.resolvers.ClassResolver$ClassUriImporter.tryImport(java.net.URI)> has generic return type <java.util.Optional<com.tngtech.archunit.core.domain.JavaClass>> with type argument depending on <com.tngtech.archunit.core.domain.JavaClass> in (ClassResolver.java:0)]

## Guava 33.2.1

- solo mecanismo: `com.google.common.base -> com.google.common.annotations` — [Class <com.google.common.base.Absent> is annotated with <com.google.common.annotations.GwtCompatible> in (Absent.java:0), Class <com.google.common.base.AbstractIterator> is annotated with <com.google.common.annotations.GwtCompatible> in (AbstractIterator.java:0)]
- solo mecanismo: `com.google.common.cache -> com.google.common.annotations` — [Class <com.google.common.cache.AbstractCache> is annotated with <com.google.common.annotations.GwtCompatible> in (AbstractCache.java:0), Class <com.google.common.cache.AbstractLoadingCache> is annotated with <com.google.common.annotations.GwtIncompatible> in (AbstractLoadingCache.java:0)]
- solo mecanismo: `com.google.common.collect -> com.google.common.annotations` — [Class <com.google.common.collect.AbstractBiMap> is annotated with <com.google.common.annotations.GwtCompatible> in (AbstractBiMap.java:0), Field <com.google.common.collect.AbstractBiMap.serialVersionUID> is annotated with <com.google.common.annotations.GwtIncompatible> in (AbstractBiMap.java:0)]
- solo mecanismo: `com.google.common.escape -> com.google.common.annotations` — [Class <com.google.common.escape.ArrayBasedCharEscaper> is annotated with <com.google.common.annotations.GwtCompatible> in (ArrayBasedCharEscaper.java:0), Class <com.google.common.escape.ArrayBasedEscaperMap> is annotated with <com.google.common.annotations.GwtCompatible> in (ArrayBasedEscaperMap.java:0)]
- solo mecanismo: `com.google.common.eventbus -> com.google.common.annotations` — [Class <com.google.common.eventbus.ElementTypesAreNonnullByDefault> is annotated with <com.google.common.annotations.GwtCompatible> in (ElementTypesAreNonnullByDefault.java:0), Class <com.google.common.eventbus.ParametricNullness> is annotated with <com.google.common.annotations.GwtCompatible> in (ParametricNullness.java:0)]
- solo mecanismo: `com.google.common.graph -> com.google.common.annotations` — [Class <com.google.common.graph.AbstractGraph> is annotated with <com.google.common.annotations.Beta> in (AbstractGraph.java:0), Class <com.google.common.graph.AbstractNetwork> is annotated with <com.google.common.annotations.Beta> in (AbstractNetwork.java:0)]
- solo mecanismo: `com.google.common.hash -> com.google.common.annotations` — [Class <com.google.common.hash.BloomFilter> is annotated with <com.google.common.annotations.Beta> in (BloomFilter.java:0), Method <com.google.common.hash.BloomFilter.bitSize()> is annotated with <com.google.common.annotations.VisibleForTesting> in (BloomFilter.java:0)]
- solo mecanismo: `com.google.common.html -> com.google.common.annotations` — [Class <com.google.common.html.ElementTypesAreNonnullByDefault> is annotated with <com.google.common.annotations.GwtCompatible> in (ElementTypesAreNonnullByDefault.java:0), Class <com.google.common.html.HtmlEscapers> is annotated with <com.google.common.annotations.GwtCompatible> in (HtmlEscapers.java:0)]
- solo mecanismo: `com.google.common.io -> com.google.common.annotations` — [Class <com.google.common.io.AppendableWriter> is annotated with <com.google.common.annotations.GwtIncompatible> in (AppendableWriter.java:0), Class <com.google.common.io.AppendableWriter> is annotated with <com.google.common.annotations.J2ktIncompatible> in (AppendableWriter.java:0)]
- solo mecanismo: `com.google.common.math -> com.google.common.annotations` — [Class <com.google.common.math.BigDecimalMath> is annotated with <com.google.common.annotations.GwtIncompatible> in (BigDecimalMath.java:0), Class <com.google.common.math.BigDecimalMath> is annotated with <com.google.common.annotations.J2ktIncompatible> in (BigDecimalMath.java:0)]
- solo mecanismo: `com.google.common.net -> com.google.common.annotations` — [Class <com.google.common.net.ElementTypesAreNonnullByDefault> is annotated with <com.google.common.annotations.GwtCompatible> in (ElementTypesAreNonnullByDefault.java:0), Class <com.google.common.net.HostAndPort> is annotated with <com.google.common.annotations.GwtCompatible> in (HostAndPort.java:0)]
- solo mecanismo: `com.google.common.primitives -> com.google.common.annotations` — [Class <com.google.common.primitives.Booleans> is annotated with <com.google.common.annotations.GwtCompatible> in (Booleans.java:0), Class <com.google.common.primitives.Booleans$BooleanArrayAsList> is annotated with <com.google.common.annotations.GwtCompatible> in (Booleans.java:0)]
- solo mecanismo: `com.google.common.reflect -> com.google.common.annotations` — [Method <com.google.common.reflect.ClassPath.getClassName(java.lang.String)> is annotated with <com.google.common.annotations.VisibleForTesting> in (ClassPath.java:0), Method <com.google.common.reflect.ClassPath.getClassPathEntries(java.lang.ClassLoader)> is annotated with <com.google.common.annotations.VisibleForTesting> in (ClassPath.java:0)]
- solo mecanismo: `com.google.common.util.concurrent -> com.google.common.annotations` — [Class <com.google.common.util.concurrent.AbstractCatchingFuture> is annotated with <com.google.common.annotations.GwtCompatible> in (AbstractCatchingFuture.java:0), Class <com.google.common.util.concurrent.AbstractExecutionThreadService> is annotated with <com.google.common.annotations.GwtIncompatible> in (AbstractExecutionThreadService.java:0)]
- solo mecanismo: `com.google.common.xml -> com.google.common.annotations` — [Class <com.google.common.xml.ElementTypesAreNonnullByDefault> is annotated with <com.google.common.annotations.GwtCompatible> in (ElementTypesAreNonnullByDefault.java:0), Class <com.google.common.xml.ParametricNullness> is annotated with <com.google.common.annotations.GwtCompatible> in (ParametricNullness.java:0)]

## DDD Sample (Citerus)

- solo JDepend: `se.citerus.dddsample.interfaces.handling.file -> se.citerus.dddsample.domain.model.handling`

