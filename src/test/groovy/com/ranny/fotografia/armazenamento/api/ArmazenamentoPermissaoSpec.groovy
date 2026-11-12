package com.ranny.fotografia.armazenamento.api

import com.ranny.fotografia.auth.model.Role
import com.ranny.fotografia.auth.model.Usuario
import com.ranny.fotografia.auth.repository.UsuarioRepository
import com.ranny.fotografia.support.JsonCorpo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import java.time.Instant

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 * Matriz de permissao: leitura para ADMIN e GESTOR, expurgo apenas para ADMIN.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = [
        "app.security.enabled=true",
        "app.auth.bootstrap-enabled=false",
        "app.auth.allow-ephemeral-keys=true",
        "app.auth.issuer=http://localhost",
        "app.auth.cookie-secure=false",
        "app.armazenamento.volume-path=\${java.io.tmpdir}",
        "app.armazenamento.reserva-bytes=1073741824"
])
class ArmazenamentoPermissaoSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    UsuarioRepository usuarioRepository

    @Autowired
    PasswordEncoder passwordEncoder

    def "anonimo nao acessa o armazenamento"() {
        expect:
        mockMvc.perform(get("/api/v1/armazenamento"))
                .andExpect(status().isUnauthorized())
    }

    def "gestor le o armazenamento mas nao expurga"() {
        given:
        def token = login("gestor@teste.com", "gestor12345", Role.GESTOR)

        expect: "leitura liberada"
        mockMvc.perform(get("/api/v1/armazenamento")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
        mockMvc.perform(get("/api/v1/armazenamento/galerias")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())

        and: "expurgo e reconciliacao bloqueados"
        mockMvc.perform(post("/api/v1/galerias/qualquer/expurgo")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([motivo: "teste", confirmacao: "teste"])))
                .andExpect(status().isForbidden())
        mockMvc.perform(post("/api/v1/armazenamento/reconciliar")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
        mockMvc.perform(post("/api/v1/armazenamento/coletar")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
    }

    def "admin passa pela autorizacao do expurgo"() {
        given:
        def token = login("admin@teste.com", "admin12345", Role.ADMIN)

        expect: "chega no servico e recebe 404 de galeria inexistente"
        mockMvc.perform(post("/api/v1/galerias/inexistente/expurgo")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([motivo: "teste", confirmacao: "teste"])))
                .andExpect(status().isNotFound())
    }

    private String login(String email, String senha, Role role) {
        if (!usuarioRepository.findByEmailIgnoreCase(email).isPresent()) {
            usuarioRepository.save(Usuario.criar(
                    email.split("@")[0], email, passwordEncoder.encode(senha),
                    role, false, Instant.now()))
        }
        def resultado = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([email: email, senha: senha])))
                .andExpect(status().isOk())
                .andReturn()
        return JsonCorpo.mapa(resultado).accessToken as String
    }
}
