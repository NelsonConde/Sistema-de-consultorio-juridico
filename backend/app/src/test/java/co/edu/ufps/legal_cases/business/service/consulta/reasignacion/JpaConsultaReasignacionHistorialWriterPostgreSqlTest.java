package co.edu.ufps.legal_cases.business.service.consulta.reasignacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.model.catalogo.Barrio;
import co.edu.ufps.legal_cases.business.model.catalogo.Departamento;
import co.edu.ufps.legal_cases.business.model.catalogo.Municipio;
import co.edu.ufps.legal_cases.business.model.catalogo.Nacionalidad;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.Tema;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.consulta.TipoResponsableConsulta;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.model.persona.Condicion;
import co.edu.ufps.legal_cases.business.model.persona.Empresa;
import co.edu.ufps.legal_cases.business.model.persona.Ocupacion;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.model.persona.TipoPersona;
import co.edu.ufps.legal_cases.business.service.consulta.reasignacion.ConsultaReasignacionHistorialWriter.RegistroReasignacion;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.support.PostgreSqlIntegrationTest;
import jakarta.persistence.EntityManager;

@SpringBootTest
class JpaConsultaReasignacionHistorialWriterPostgreSqlTest
        extends PostgreSqlIntegrationTest {

    private static final String ESQUEMA = "\"DB_consultorioJuridico\"";
    private static final String TABLA = ESQUEMA + ".consulta_reasignacion_historial";
    private static final Path MIGRACIONES = Path.of("db", "migration");

    @Autowired
    private ConsultaReasignacionHistorialWriter historialWriter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private DatosPrueba datos;

    @BeforeEach
    void prepararEsquemaYDatos() {
        transactionTemplate = new TransactionTemplate(transactionManager);

        // El contenedor es efímero. Se elimina la tabla generada por Hibernate para
        // comprobar que la definición efectiva provenga de la migración versionada.
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + TABLA);
        ejecutarScript("V20261011_01__create_consulta_reasignacion_historial.sql");

        datos = transactionTemplate.execute(estado -> crearDatosPrueba());
        assertNotNull(datos);
    }

    @AfterEach
    void limpiarObjetosDeMigracion() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + TABLA + " CASCADE; "
                + "DROP FUNCTION IF EXISTS " + ESQUEMA
                + ".validar_responsables_reasignacion_consulta(); "
                + "DROP FUNCTION IF EXISTS " + ESQUEMA
                + ".prevenir_modificacion_historial_reasignacion();");
    }

    @ParameterizedTest
    @EnumSource(TipoResponsableConsulta.class)
    void debePersistirTodosLosCamposParaCadaTipo(
            TipoResponsableConsulta tipoResponsable) {

        String motivo = "  Validación persistente " + tipoResponsable + "  ";
        Instant inicio = Instant.now();

        persistir(tipoResponsable, datos.anterior(tipoResponsable),
                datos.nuevo(tipoResponsable), motivo);

        Instant fin = Instant.now();
        Map<String, Object> fila = jdbcTemplate.queryForMap(
                "SELECT * FROM " + TABLA + " WHERE motivo = ?",
                motivo.trim());

        assertEquals(datos.consultaId(), numero(fila.get("consulta_id")));
        assertEquals(tipoResponsable.name(), fila.get("tipo_responsable"));
        assertEquals(datos.anterior(tipoResponsable),
                numero(fila.get("responsable_anterior_id")));
        assertEquals(datos.nuevo(tipoResponsable),
                numero(fila.get("responsable_nuevo_id")));
        assertEquals(motivo.trim(), fila.get("motivo"));
        assertEquals(datos.actorUsuarioId(), numero(fila.get("actor_usuario_id")));

        Instant ocurridoEn = ((Timestamp) fila.get("ocurrido_en")).toInstant();
        assertTrue(!ocurridoEn.isBefore(inicio.minusSeconds(1))
                && !ocurridoEn.isAfter(fin.plusSeconds(1)));
        assertEquals(1L, contarPorMotivo(motivo.trim()));
    }

    @Test
    void debeConservarResponsableAnteriorNulo() {
        String motivo = motivoUnico("sin-anterior");

        persistir(TipoResponsableConsulta.MONITOR, null,
                datos.monitorNuevoId(), motivo);

        Long anterior = jdbcTemplate.queryForObject(
                "SELECT responsable_anterior_id FROM " + TABLA
                        + " WHERE motivo = ?",
                Long.class, motivo);

        assertEquals(null, anterior);
    }

    @Test
    void debeConservarMultiplesEventosSucesivos() {
        String primerMotivo = motivoUnico("sucesivo-1");
        String segundoMotivo = motivoUnico("sucesivo-2");

        persistir(TipoResponsableConsulta.MONITOR,
                datos.monitorAnteriorId(), datos.monitorNuevoId(), primerMotivo);
        persistir(TipoResponsableConsulta.MONITOR,
                datos.monitorNuevoId(), datos.monitorTerceroId(), segundoMotivo);

        assertEquals(1L, contarPorMotivo(primerMotivo));
        assertEquals(1L, contarPorMotivo(segundoMotivo));
    }

    @Test
    void debeAplicarRestriccionesYValidacionPolimorfica() {
        assertThrows(DataAccessException.class, () -> insertarCrudo(
                datos.consultaId(), "INVALIDO", null,
                datos.monitorNuevoId(), "tipo inválido", datos.actorUsuarioId()));

        assertThrows(DataAccessException.class, () -> insertarCrudo(
                datos.consultaId(), "MONITOR", null,
                datos.monitorNuevoId(), "   ", datos.actorUsuarioId()));

        assertThrows(DataAccessException.class, () -> insertarCrudo(
                datos.consultaId(), "MONITOR", null,
                Long.MAX_VALUE, "responsable inexistente", datos.actorUsuarioId()));

        assertThrows(DataAccessException.class, () -> insertarCrudo(
                Long.MAX_VALUE, "MONITOR", null,
                datos.monitorNuevoId(), "consulta inexistente", datos.actorUsuarioId()));

        assertThrows(DataAccessException.class, () -> insertarCrudo(
                datos.consultaId(), "MONITOR", null,
                datos.monitorNuevoId(), "actor inexistente", Long.MAX_VALUE));
    }

    @Test
    void debeRechazarUpdateYDeleteEnPostgreSql() {
        String motivo = motivoUnico("inmutable");
        persistir(TipoResponsableConsulta.ASESOR,
                datos.asesorAnteriorId(), datos.asesorNuevoId(), motivo);

        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM " + TABLA + " WHERE motivo = ?",
                Long.class, motivo);

        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "UPDATE " + TABLA + " SET motivo = ? WHERE id = ?",
                "alterado", id));
        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "DELETE FROM " + TABLA + " WHERE id = ?", id));

        assertEquals(1L, contarPorMotivo(motivo));
    }

    @Test
    void debeExigirTransaccionExistenteYParticiparEnSuRollback() {
        String motivo = motivoUnico("rollback");

        assertThrows(IllegalTransactionStateException.class, () -> historialWriter.registrar(
                registro(TipoResponsableConsulta.ESTUDIANTE,
                        datos.estudianteAnteriorId(), datos.estudianteNuevoId(), motivo)));

        assertThrows(FalloPosterior.class, () -> transactionTemplate.execute(estado -> {
            historialWriter.registrar(registro(TipoResponsableConsulta.ESTUDIANTE,
                    datos.estudianteAnteriorId(), datos.estudianteNuevoId(), motivo));
            throw new FalloPosterior();
        }));

        assertEquals(0L, contarPorMotivo(motivo));
    }

    @Test
    void scriptDeVerificacionDebeConfirmarLaMigracion() {
        ejecutarScript(
                "V20261011_01__create_consulta_reasignacion_historial_verify.sql");
    }

    private void persistir(
            TipoResponsableConsulta tipoResponsable,
            Long responsableAnteriorId,
            Long responsableNuevoId,
            String motivo) {

        transactionTemplate.executeWithoutResult(estado -> historialWriter.registrar(
                registro(tipoResponsable, responsableAnteriorId,
                        responsableNuevoId, motivo)));
    }

    private RegistroReasignacion registro(
            TipoResponsableConsulta tipoResponsable,
            Long responsableAnteriorId,
            Long responsableNuevoId,
            String motivo) {

        return new RegistroReasignacion(
                datos.consultaId(), tipoResponsable, responsableAnteriorId,
                responsableNuevoId, motivo, datos.actorUsuarioId());
    }

    private void insertarCrudo(
            Long consultaId,
            String tipoResponsable,
            Long responsableAnteriorId,
            Long responsableNuevoId,
            String motivo,
            Long actorUsuarioId) {

        jdbcTemplate.update(
                "INSERT INTO " + TABLA + " (consulta_id, tipo_responsable, "
                        + "responsable_anterior_id, responsable_nuevo_id, motivo, "
                        + "actor_usuario_id) VALUES (?, ?, ?, ?, ?, ?)",
                consultaId, tipoResponsable, responsableAnteriorId,
                responsableNuevoId, motivo, actorUsuarioId);
    }

    private long contarPorMotivo(String motivo) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + TABLA + " WHERE motivo = ?",
                Long.class, motivo);
    }

    private void ejecutarScript(String nombre) {
        Path script = MIGRACIONES.resolve(nombre).toAbsolutePath();
        try {
            jdbcTemplate.execute(Files.readString(script));
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "No fue posible leer el script " + script, exception);
        }
    }

    private DatosPrueba crearDatosPrueba() {
        String lote = UUID.randomUUID().toString().substring(0, 8);

        Sede sede = new Sede();
        sede.setNombre("Sede historial " + lote);
        entityManager.persist(sede);

        Area area = new Area();
        area.setNombre("Área historial " + lote);
        entityManager.persist(area);

        TipoDocumento tipoDocumento = new TipoDocumento();
        tipoDocumento.setNombre("Documento historial " + lote);
        entityManager.persist(tipoDocumento);

        Asesor asesorAnterior = crearAsesor("ant-" + lote, sede, area, tipoDocumento);
        Asesor asesorNuevo = crearAsesor("nvo-" + lote, sede, area, tipoDocumento);
        Estudiante estudianteAnterior = crearEstudiante(
                "ant-" + lote, sede, tipoDocumento, asesorAnterior);
        Estudiante estudianteNuevo = crearEstudiante(
                "nvo-" + lote, sede, tipoDocumento, asesorAnterior);
        Monitor monitorAnterior = crearMonitor("ant-" + lote, sede, tipoDocumento);
        Monitor monitorNuevo = crearMonitor("nvo-" + lote, sede, tipoDocumento);
        Monitor monitorTercero = crearMonitor("ter-" + lote, sede, tipoDocumento);

        Tema tema = new Tema();
        tema.setNombre("Tema historial " + lote);
        tema.setArea(area);
        entityManager.persist(tema);

        Persona persona = crearPersona(lote);
        Consulta consulta = new Consulta();
        consulta.setFecha(LocalDate.now());
        consulta.setDescripcion("Consulta para historial de reasignaciones");
        consulta.setHechos("Hechos de prueba");
        consulta.setPretensiones("Pretensiones de prueba");
        consulta.setConceptoJuridico("Concepto de prueba");
        consulta.setTramite("Asesoría");
        consulta.setEstado(EstadoConsulta.ACTIVO);
        consulta.setPersona(persona);
        consulta.setSede(sede);
        consulta.setArea(area);
        consulta.setTema(tema);
        entityManager.persist(consulta);

        Rol rol = new Rol();
        rol.setNombre("ROL_HISTORIAL_" + lote);
        rol.setTipoPerfil(TipoPerfilUsuario.ADMINISTRATIVO);
        entityManager.persist(rol);

        UsuarioSistema actor = new UsuarioSistema();
        actor.setUsername("actor." + lote + "@historial.test");
        actor.setPasswordHash("hash-solo-prueba");
        actor.setTipoPerfilActual(TipoPerfilUsuario.ADMINISTRATIVO);
        actor.setRol(rol);
        entityManager.persist(actor);

        entityManager.flush();
        return new DatosPrueba(
                consulta.getId(), actor.getId(),
                estudianteAnterior.getId(), estudianteNuevo.getId(),
                asesorAnterior.getId(), asesorNuevo.getId(),
                monitorAnterior.getId(), monitorNuevo.getId(), monitorTercero.getId());
    }

    private Asesor crearAsesor(
            String sufijo,
            Sede sede,
            Area area,
            TipoDocumento tipoDocumento) {

        Asesor asesor = new Asesor();
        asesor.setNombre("Asesor " + sufijo);
        asesor.setTipoDocumento(tipoDocumento);
        asesor.setDocumento("ASE-DOC-" + sufijo);
        asesor.setEmail("asesor." + sufijo + "@historial.test");
        asesor.setTelefono("ASE-TEL-" + sufijo);
        asesor.setUsuario("asesor-" + sufijo);
        asesor.setSede(sede);
        asesor.setCodigo("ASE-COD-" + sufijo);
        asesor.setArea(area);
        entityManager.persist(asesor);
        return asesor;
    }

    private Estudiante crearEstudiante(
            String sufijo,
            Sede sede,
            TipoDocumento tipoDocumento,
            Asesor asesor) {

        Estudiante estudiante = new Estudiante();
        estudiante.setNombre("Estudiante " + sufijo);
        estudiante.setTipoDocumento(tipoDocumento);
        estudiante.setDocumento("EST-DOC-" + sufijo);
        estudiante.setEmail("estudiante." + sufijo + "@historial.test");
        estudiante.setTelefono("EST-TEL-" + sufijo);
        estudiante.setUsuario("estudiante-" + sufijo);
        estudiante.setSede(sede);
        estudiante.setCodigo("EST-COD-" + sufijo);
        estudiante.setAsesor(asesor);
        entityManager.persist(estudiante);
        return estudiante;
    }

    private Monitor crearMonitor(
            String sufijo,
            Sede sede,
            TipoDocumento tipoDocumento) {

        Monitor monitor = new Monitor();
        monitor.setNombre("Monitor " + sufijo);
        monitor.setTipoDocumento(tipoDocumento);
        monitor.setDocumento("MON-DOC-" + sufijo);
        monitor.setEmail("monitor." + sufijo + "@historial.test");
        monitor.setTelefono("MON-TEL-" + sufijo);
        monitor.setUsuario("monitor-" + sufijo);
        monitor.setCodigo("MON-COD-" + sufijo);
        monitor.setSede(sede);
        entityManager.persist(monitor);
        return monitor;
    }

    private Persona crearPersona(String lote) {
        Departamento departamento = new Departamento();
        departamento.setNombre("Departamento " + lote);
        entityManager.persist(departamento);

        Municipio municipio = new Municipio();
        municipio.setNombre("Municipio " + lote);
        municipio.setDepartamento(departamento);
        entityManager.persist(municipio);

        Barrio barrio = new Barrio();
        barrio.setNombre("Barrio " + lote);
        barrio.setMunicipio(municipio);
        entityManager.persist(barrio);

        Nacionalidad nacionalidad = new Nacionalidad();
        nacionalidad.setNombre("Nacionalidad " + lote);
        entityManager.persist(nacionalidad);

        TipoPersona tipoPersona = new TipoPersona();
        tipoPersona.setNombre("Solicitante " + lote);
        entityManager.persist(tipoPersona);

        Condicion condicion = new Condicion();
        condicion.setNombre("Condición " + lote);
        entityManager.persist(condicion);

        Ocupacion ocupacion = new Ocupacion();
        ocupacion.setNombre("Ocupación " + lote);
        entityManager.persist(ocupacion);

        Empresa empresa = new Empresa();
        empresa.setNombre("Empresa " + lote);
        entityManager.persist(empresa);

        Persona persona = new Persona();
        persona.setTipoPersona(tipoPersona);
        persona.setTipoDocumento("CC");
        persona.setNumeroDocumento("HIST-" + lote);
        persona.setFechaExpedicion(LocalDate.of(2010, 1, 1));
        persona.setCiudadExpedicion("Cúcuta");
        persona.setNombres("Persona");
        persona.setApellidos("Historial");
        persona.setNombreIdentitario("Persona");
        persona.setPronombre("ella");
        persona.setSexo("mujer");
        persona.setGenero("femenino");
        persona.setOrientacionSexual("no informa");
        persona.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        persona.setTelefono("TEL-" + lote);
        persona.setCorreo("persona." + lote + "@historial.test");
        persona.setNacionalidad(nacionalidad);
        persona.setEstadoCivil("soltero");
        persona.setEscolaridad("universitaria");
        persona.setGrupoEtnico("no informa");
        persona.setCondicionActual(condicion);
        persona.setSabeLeerEscribir(true);
        persona.setDiscapacidad("ninguna");
        persona.setCaracterizacionPcd("no aplica");
        persona.setNecesitaAjustePcd(false);
        persona.setMunicipio(municipio);
        persona.setBarrio(barrio);
        persona.setDireccion("Dirección de prueba");
        persona.setComuna("Uno");
        persona.setLocalidad("Centro");
        persona.setEstrato(2);
        persona.setTipoVivienda("arrendada");
        persona.setZona("urbana");
        persona.setTenencia("arrendatario");
        persona.setNumeroPersonasACargo(0);
        persona.setIngresosAdicionales(false);
        persona.setEnergiaElectrica(true);
        persona.setAcueducto(true);
        persona.setAlcantarillado(true);
        persona.setOcupacion(ocupacion);
        persona.setEmpresa(empresa);
        persona.setSalario(0);
        persona.setCargo("No aplica");
        persona.setDireccionEmpresa("No aplica");
        persona.setTelefonoEmpresa("0000000000");
        persona.setComoSeEntero("Universidad");
        persona.setRelacionConUniversidad("Ninguna");
        persona.setActivo(true);
        entityManager.persist(persona);
        return persona;
    }

    private static Long numero(Object valor) {
        return valor == null ? null : ((Number) valor).longValue();
    }

    private static String motivoUnico(String prefijo) {
        return prefijo + "-" + UUID.randomUUID();
    }

    private record DatosPrueba(
            Long consultaId,
            Long actorUsuarioId,
            Long estudianteAnteriorId,
            Long estudianteNuevoId,
            Long asesorAnteriorId,
            Long asesorNuevoId,
            Long monitorAnteriorId,
            Long monitorNuevoId,
            Long monitorTerceroId) {

        Long anterior(TipoResponsableConsulta tipo) {
            return switch (tipo) {
                case ESTUDIANTE -> estudianteAnteriorId;
                case ASESOR -> asesorAnteriorId;
                case MONITOR -> monitorAnteriorId;
            };
        }

        Long nuevo(TipoResponsableConsulta tipo) {
            return switch (tipo) {
                case ESTUDIANTE -> estudianteNuevoId;
                case ASESOR -> asesorNuevoId;
                case MONITOR -> monitorNuevoId;
            };
        }
    }

    private static final class FalloPosterior extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
