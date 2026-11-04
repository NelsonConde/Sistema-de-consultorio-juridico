ALTER TABLE "DB_consultorioJuridico".usuario_sistema
    ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE "DB_consultorioJuridico".usuario_sistema
    ADD COLUMN estado_activacion VARCHAR(20);

UPDATE "DB_consultorioJuridico".usuario_sistema
SET estado_activacion = 'ACTIVA'
WHERE estado_activacion IS NULL;

ALTER TABLE "DB_consultorioJuridico".usuario_sistema
    ALTER COLUMN estado_activacion SET NOT NULL;

ALTER TABLE "DB_consultorioJuridico".usuario_sistema
    ADD CONSTRAINT ck_usuario_sistema_estado_activacion
        CHECK (estado_activacion IN ('PENDIENTE', 'ACTIVA'));

CREATE TABLE "DB_consultorioJuridico".account_activation_token (
                                                                   id BIGSERIAL PRIMARY KEY,
                                                                   token_hash VARCHAR(64) NOT NULL,
                                                                   usuario_sistema_id BIGINT NOT NULL,
                                                                   fecha_creacion TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                                                                   fecha_expiracion TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                                                                   usado BOOLEAN NOT NULL DEFAULT FALSE,
                                                                   fecha_uso TIMESTAMP WITHOUT TIME ZONE,
                                                                   revocado BOOLEAN NOT NULL DEFAULT FALSE,
                                                                   fecha_revocacion TIMESTAMP WITHOUT TIME ZONE,

                                                                   CONSTRAINT uk_account_activation_token_hash
                                                                       UNIQUE (token_hash),

                                                                   CONSTRAINT fk_account_activation_token_usuario
                                                                       FOREIGN KEY (usuario_sistema_id)
                                                                           REFERENCES "DB_consultorioJuridico".usuario_sistema(id),

                                                                   CONSTRAINT ck_account_activation_token_expiracion
                                                                       CHECK (fecha_expiracion > fecha_creacion),

                                                                   CONSTRAINT ck_account_activation_token_estado_terminal
                                                                       CHECK (NOT (usado AND revocado)),

                                                                   CONSTRAINT ck_account_activation_token_fecha_uso
                                                                       CHECK (
                                                                           (usado AND fecha_uso IS NOT NULL)
                                                                               OR
                                                                           (NOT usado AND fecha_uso IS NULL)
                                                                           ),

                                                                   CONSTRAINT ck_account_activation_token_fecha_revocacion
                                                                       CHECK (
                                                                           (revocado AND fecha_revocacion IS NOT NULL)
                                                                               OR
                                                                           (NOT revocado AND fecha_revocacion IS NULL)
                                                                           )
);

CREATE INDEX ix_account_activation_token_usuario
    ON "DB_consultorioJuridico".account_activation_token(usuario_sistema_id);

CREATE UNIQUE INDEX uk_account_activation_token_activo_por_usuario
    ON "DB_consultorioJuridico".account_activation_token(usuario_sistema_id)
    WHERE usado = FALSE
      AND revocado = FALSE;