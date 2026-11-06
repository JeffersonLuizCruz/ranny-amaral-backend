package com.ranny.fotografia

import org.springframework.modulith.core.ApplicationModules
import spock.lang.Specification

/**
 * Garante que os modulos nao violem as fronteiras declaradas pelo Spring
 * Modulith. Roda sem contexto Spring: a analise e estatica.
 */
class ModulithArchitectureSpec extends Specification {

    def "modulos respeitam as fronteiras do Spring Modulith"() {
        given:
        def modulos = ApplicationModules.of(FotografiaApplication)

        expect:
        modulos.verify()
    }
}
