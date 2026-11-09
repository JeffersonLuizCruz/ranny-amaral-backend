package com.ranny.fotografia.auth.model;

/** Tipos de evento de autenticacao auditados. */
public enum AuthEventoTipo {
    LOGIN_SUCESSO,
    LOGIN_FALHA,
    LOGIN_BLOQUEADO,
    LOGOUT,
    SENHA_ALTERADA,
    SENHA_RESETADA_POR_ADMIN,
    USUARIO_CRIADO,
    USUARIO_ATUALIZADO,
    USUARIO_ATIVADO,
    USUARIO_DESATIVADO,
    REFRESH_ROTACIONADO,
    REFRESH_REUTILIZADO
}
