package org.malphas.proxy.config

import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml
import java.io.File

fun loadConfig(): ServiceConfiguration {
    val yaml = Yaml {}

    var fcfg = File("config.yaml")
    if (!fcfg.exists())
        fcfg = File("config.yml")

    val cfg = fcfg.readText()

    val config: ServiceConfiguration = yaml.decodeFromString(cfg)
    return config
}