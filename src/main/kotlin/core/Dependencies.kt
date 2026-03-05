package core

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.DependencyRegistry
import io.ktor.server.plugins.di.dependencies

fun Application.dynamicDependencies(
    block: DynamicDependenciesScope.() -> DynamicDependenciesScope
) {
    DynamicDependenciesScope(isLocal(), this).block()
}

class DynamicDependenciesScope(private val isLocal: Boolean, private val application: Application) {
    fun local(block: DependencyRegistry.() -> Unit): DynamicDependenciesScope {
        if (isLocal) application.dependencies(block)
        return this
    }

    fun cloud(block: DependencyRegistry.() -> Unit): DynamicDependenciesScope {
        if (!isLocal) application.dependencies(block)
        return this
    }
}
