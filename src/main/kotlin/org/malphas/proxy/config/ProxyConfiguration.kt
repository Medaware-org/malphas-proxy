package org.malphas.proxy.config

import kotlinx.serialization.Serializable

@Serializable
data class MalphasBackendConfig(val host: String, val port: Int)

@Serializable
data class MalphasProxyConfig(val malphas: MalphasBackendConfig)