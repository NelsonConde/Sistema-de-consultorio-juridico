CREATE TABLE IF NOT EXISTS "DB_consultorioJuridico".periodo_academico (
                                                                          id BIGSERIAL PRIMARY KEY,
                                                                          anio INTEGER NOT NULL,
                                                                          semestre INTEGER NOT NULL,
                                                                          fecha_inicio DATE NOT NULL,
                                                                          fecha_fin DATE NOT NULL,
                                                                          activo BOOLEAN NOT NULL DEFAULT TRUE,

                                                                          CONSTRAINT uk_periodo_academico_anio_semestre
                                                                          UNIQUE (anio, semestre),

    CONSTRAINT chk_periodo_academico_semestre
    CHECK (semestre IN (1, 2)),

    CONSTRAINT chk_periodo_academico_fechas
    CHECK (fecha_inicio <= fecha_fin)
    );

INSERT INTO "DB_consultorioJuridico".periodo_academico
(anio, semestre, fecha_inicio, fecha_fin, activo)
VALUES
    (2024, 1, DATE '2024-01-01', DATE '2024-06-30', TRUE),
    (2024, 2, DATE '2024-07-01', DATE '2024-12-31', TRUE),
    (2025, 1, DATE '2025-01-01', DATE '2025-06-30', TRUE),
    (2025, 2, DATE '2025-07-01', DATE '2025-12-31', TRUE),
    (2026, 1, DATE '2026-01-01', DATE '2026-06-30', TRUE),
    (2026, 2, DATE '2026-07-01', DATE '2026-12-31', TRUE)
    ON CONFLICT (anio, semestre) DO NOTHING;