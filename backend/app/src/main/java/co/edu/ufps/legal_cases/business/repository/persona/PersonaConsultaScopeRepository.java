package co.edu.ufps.legal_cases.business.repository.persona;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;

/**
 * Consultas de existencia usadas exclusivamente para autorizar el detalle de
 * una persona a partir de consultas juridicas no archivadas.
 */
public interface PersonaConsultaScopeRepository extends Repository<Consulta, Long> {

    @Query("""
            SELECT CASE WHEN COUNT(c.id) > 0 THEN true ELSE false END
            FROM Consulta c
            WHERE c.estado <> :estadoArchivado
              AND (
                    c.persona.id = :personaId
                    OR EXISTS (
                        SELECT parte.id
                        FROM Consulta consultaParte
                        JOIN consultaParte.partes parte
                        WHERE consultaParte.id = c.id
                          AND parte.id = :personaId
                    )
                    OR EXISTS (
                        SELECT contraparte.id
                        FROM Consulta consultaContraparte
                        JOIN consultaContraparte.contrapartes contraparte
                        WHERE consultaContraparte.id = c.id
                          AND contraparte.id = :personaId
                    )
              )
              AND c.estudiante.id = :perfilId
            """)
    boolean existsPersonaEnConsultaDeEstudiante(
            @Param("personaId") Long personaId,
            @Param("perfilId") Long perfilId,
            @Param("estadoArchivado") EstadoConsulta estadoArchivado);

    @Query("""
            SELECT CASE WHEN COUNT(c.id) > 0 THEN true ELSE false END
            FROM Consulta c
            LEFT JOIN c.estudiante estudiante
            LEFT JOIN estudiante.asesor asesorDelEstudiante
            WHERE c.estado <> :estadoArchivado
              AND (
                    c.persona.id = :personaId
                    OR EXISTS (
                        SELECT parte.id
                        FROM Consulta consultaParte
                        JOIN consultaParte.partes parte
                        WHERE consultaParte.id = c.id
                          AND parte.id = :personaId
                    )
                    OR EXISTS (
                        SELECT contraparte.id
                        FROM Consulta consultaContraparte
                        JOIN consultaContraparte.contrapartes contraparte
                        WHERE consultaContraparte.id = c.id
                          AND contraparte.id = :personaId
                    )
              )
              AND (
                    c.asesor.id = :perfilId
                    OR asesorDelEstudiante.id = :perfilId
              )
            """)
    boolean existsPersonaEnConsultaDeAsesor(
            @Param("personaId") Long personaId,
            @Param("perfilId") Long perfilId,
            @Param("estadoArchivado") EstadoConsulta estadoArchivado);

    @Query("""
            SELECT CASE WHEN COUNT(c.id) > 0 THEN true ELSE false END
            FROM Consulta c
            WHERE c.estado <> :estadoArchivado
              AND (
                    c.persona.id = :personaId
                    OR EXISTS (
                        SELECT parte.id
                        FROM Consulta consultaParte
                        JOIN consultaParte.partes parte
                        WHERE consultaParte.id = c.id
                          AND parte.id = :personaId
                    )
                    OR EXISTS (
                        SELECT contraparte.id
                        FROM Consulta consultaContraparte
                        JOIN consultaContraparte.contrapartes contraparte
                        WHERE consultaContraparte.id = c.id
                          AND contraparte.id = :personaId
                    )
              )
              AND c.monitor.id = :perfilId
            """)
    boolean existsPersonaEnConsultaDeMonitor(
            @Param("personaId") Long personaId,
            @Param("perfilId") Long perfilId,
            @Param("estadoArchivado") EstadoConsulta estadoArchivado);
}