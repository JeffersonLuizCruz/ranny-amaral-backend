package com.ranny.fotografia.auth.api

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = [
        "app.security.enabled=true",
        "app.auth.bootstrap-enabled=false",
        "app.auth.allow-ephemeral-keys=true",
        "app.auth.issuer=http://localhost",
        "app.auth.cookie-secure=false"
])
class AuthControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    UsuarioRepository usuarioRepository

    @Autowired
    PasswordEncoder passwordEncoder

    def "login valido devolve access token e perfil"() {
        given:
        criarUsuario("Admin", "admin@teste.com", "admin12345", Role.ADMIN, false)

        when:
        def resultado = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([email: "admin@teste.com", senha: "admin12345"])))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andReturn()

        then:
        def json = JsonCorpo.mapa(resultado)
        json.accessToken
        json.usuario.email == "admin@teste.com"
        json.usuario.role == "admin"
        !json.deveTrocarSenha
    }

    def "login invalido responde 401 e bloqueia apos cinco tentativas"() {
        given:
        criarUsuario("Admin", "admin@teste.com", "admin12345", Role.ADMIN, false)

        when:
        5.times {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonCorpo.json([email: "admin@teste.com", senha: "errada"])))
                    .andExpect(status().isUnauthorized())
        }
        def bloqueado = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([email: "admin@teste.com", senha: "admin12345"])))
                .andReturn()

        then:
        bloqueado.response.status == 429
        JsonCorpo.mapa(bloqueado).codigo == "CONTA_BLOQUEADA"
    }

    def "rota administrativa exige JWT"() {
        expect:
        mockMvc.perform(get("/api/v1/clientes")).andExpect(status().isUnauthorized())

        when:
        def token = login("admin@teste.com", "admin12345", Role.ADMIN)
        def resultado = mockMvc.perform(get("/api/v1/clientes")
                .header("Authorization", "Bearer " + token))
                .andReturn()

        then:
        resultado.response.status == 200
    }

    def "gestor acessa o negocio mas nao a gestao de usuarios"() {
        given:
        def token = login("ranny@teste.com", "ranny12345", Role.GESTOR)

        expect:
        mockMvc.perform(get("/api/v1/clientes")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())

        and:
        mockMvc.perform(get("/api/v1/usuarios")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
    }

    def "primeiro login forca a troca de senha"() {
        given:
        criarUsuario("Ranny", "ranny@teste.com", "ranny12345", Role.GESTOR, true)
        def token = login("ranny@teste.com", "ranny12345", Role.GESTOR)

        when:
        def bloqueado = mockMvc.perform(get("/api/v1/clientes")
                .header("Authorization", "Bearer " + token))
                .andReturn()

        then:
        bloqueado.response.status == 403
        JsonCorpo.mapa(bloqueado).codigo == "TROCA_SENHA_OBRIGATORIA"

        when:
        def troca = mockMvc.perform(post("/api/v1/auth/change-password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([senhaAtual: "ranny12345", novaSenha: "novaSenha123"])))
                .andExpect(status().isOk())
                .andReturn()

        then:
        !JsonCorpo.mapa(troca).deveTrocarSenha

        when:
        def depois = mockMvc.perform(get("/api/v1/clientes")
                .header("Authorization", "Bearer " + JsonCorpo.mapa(troca).accessToken))
                .andReturn()

        then:
        depois.response.status == 200
    }

    def "admin cria usuario e recebe senha temporaria uma vez"() {
        given:
        def token = login("admin@teste.com", "admin12345", Role.ADMIN)

        when:
        def resultado = mockMvc.perform(post("/api/v1/usuarios")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([nome: "Nova Pessoa", email: "nova@teste.com", role: "gestor"])))
                .andExpect(status().isCreated())
                .andReturn()

        then:
        def json = JsonCorpo.mapa(resultado)
        json.senhaTemporaria
        json.usuario.deveTrocarSenha
        usuarioRepository.findByEmailIgnoreCase("nova@teste.com").isPresent()
    }

    def "refresh rotaciona e reutilizacao revoga a sessao"() {
        given:
        criarUsuario("Admin", "admin@teste.com", "admin12345", Role.ADMIN, false)
        def loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([email: "admin@teste.com", senha: "admin12345"])))
                .andReturn()
        def cookie = loginResult.response.getHeader("Set-Cookie").split(";")[0]
        def refreshToken = cookie.split("=")[1]

        when:
        def primeiro = mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshToken)))
                .andExpect(status().isOk())
                .andReturn()
        def segundo = mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshToken)))
                .andReturn()

        then:
        JsonCorpo.mapa(primeiro).accessToken
        segundo.response.status == 401
    }

    private String login(String email, String senha, Role role) {
        if (!usuarioRepository.findByEmailIgnoreCase(email).isPresent()) {
            criarUsuario(email.split("@")[0], email, senha, role, false)
        }
        def resultado = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonCorpo.json([email: email, senha: senha])))
                .andExpect(status().isOk())
                .andReturn()
        return JsonCorpo.mapa(resultado).accessToken as String
    }

    private Usuario criarUsuario(String nome, String email, String senha, Role role,
                                 boolean deveTrocar) {
        return usuarioRepository.save(Usuario.criar(nome, email, passwordEncoder.encode(senha),
                role, deveTrocar, Instant.now()))
    }
}
