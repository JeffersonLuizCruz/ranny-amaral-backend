package com.ranny.fotografia.auth.api;

import com.ranny.fotografia.auth.model.Role;
import com.ranny.fotografia.auth.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** Contratos HTTP do modulo de autenticacao e usuarios. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "nao pode ser vazio")
            @Email(message = "deve ser um e-mail valido")
            @Size(max = 255, message = "deve ter no maximo 255 caracteres")
            String email,

            @NotBlank(message = "nao pode ser vazio")
            @Size(max = 128, message = "deve ter no maximo 128 caracteres")
            String senha
    ) {
    }

    public record AlterarSenhaRequest(
            @NotBlank(message = "nao pode ser vazio")
            @Size(max = 128, message = "deve ter no maximo 128 caracteres")
            String senhaAtual,

            @NotBlank(message = "nao pode ser vazio")
            @Size(min = 8, max = 128, message = "deve ter entre 8 e 128 caracteres")
            String novaSenha
    ) {
    }

    public record CriarUsuarioRequest(
            @NotBlank(message = "nao pode ser vazio")
            @Size(max = 160, message = "deve ter no maximo 160 caracteres")
            String nome,

            @NotBlank(message = "nao pode ser vazio")
            @Email(message = "deve ser um e-mail valido")
            @Size(max = 255, message = "deve ter no maximo 255 caracteres")
            String email,

            @NotNull(message = "nao pode ser nulo")
            Role role
    ) {
    }

    public record AtualizarUsuarioRequest(
            @NotBlank(message = "nao pode ser vazio")
            @Size(max = 160, message = "deve ter no maximo 160 caracteres")
            String nome,

            @NotNull(message = "nao pode ser nulo")
            Role role
    ) {
    }

    public record AtivoRequest(
            @NotNull(message = "nao pode ser nulo")
            Boolean ativo
    ) {
    }

    public record UsuarioResponse(
            String id,
            String nome,
            String email,
            Role role,
            boolean ativo,
            boolean deveTrocarSenha,
            Instant ultimoLoginEm,
            Instant createdAt,
            Instant updatedAt
    ) {

        public static UsuarioResponse de(Usuario usuario) {
            return new UsuarioResponse(
                    usuario.getId(),
                    usuario.getNome(),
                    usuario.getEmail(),
                    usuario.getRole(),
                    usuario.isAtivo(),
                    usuario.isDeveTrocarSenha(),
                    usuario.getUltimoLoginEm(),
                    usuario.getAuditoria().getCriadoEm(),
                    usuario.getAuditoria().getAtualizadoEm());
        }

        public static List<UsuarioResponse> de(List<Usuario> usuarios) {
            return usuarios.stream().map(UsuarioResponse::de).toList();
        }
    }

    public record SessaoResponse(
            String accessToken,
            long expiresInSeconds,
            UsuarioResponse usuario,
            boolean deveTrocarSenha
    ) {
    }

    public record SenhaTemporariaResponse(
            UsuarioResponse usuario,
            String senhaTemporaria
    ) {
    }
}
