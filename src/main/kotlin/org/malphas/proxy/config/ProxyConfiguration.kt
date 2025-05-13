package org.malphas.proxy.config

import kotlinx.serialization.Serializable

@Serializable
data class MalphasBackendConfig(val host: String, val port: Int)

@Serializable
data class MalphasAuthConfig(val redirect: String)

@Serializable
data class ProxyConfig(val pass: List<String>)

@Serializable
data class FrontendConfig(val origin: String)

@Serializable
data class ServiceConfiguration(val backend: MalphasBackendConfig, val auth: MalphasAuthConfig, val proxy: ProxyConfig, val frontend: FrontendConfig)

