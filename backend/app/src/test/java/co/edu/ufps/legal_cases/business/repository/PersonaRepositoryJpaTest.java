package co.edu.ufps.legal_cases.business.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.model.catalogo.Barrio;
import co.edu.ufps.legal_cases.business.model.catalogo.Departamento;
import co.edu.ufps.legal_cases.business.model.catalogo.Municipio;
import co.edu.ufps.legal_cases.business.model.catalogo.Nacionalidad;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.Tema;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.conciliacion.Conciliacion;
import co.edu.ufps.legal_cases.business.model.conciliacion.EstadoConciliacion;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Conciliador;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.model.perfil.TipoConciliador;
import co.edu.ufps.legal_cases.business.model.persona.Condicion;
import co.edu.ufps.legal_cases.business.model.persona.Empresa;
import co.edu.ufps.legal_cases.business.model.persona.Ocupacion;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.model.persona.TipoPersona;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaConsultaScopeRepository;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaRepository;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaResumenProjection;
import co.edu.ufps.legal_cases.support.PostgreSqlIntegrationTest;
import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=\"DB_consultorioJuridico\"",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE)
class PersonaRepositoryJpaTest extends PostgreSqlIntegrationTest {

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private PersonaConsultaScopeRepository personaConsultaScopeRepository;

    @Autowired
    private EntityManager entityManager;

    private TipoPersona tipoPersona;
    private Nacionalidad nacionalidad;
    private Condicion condicion;
    private Municipio municipio;
    private Barrio barrio;
    private Ocupacion ocupacion;
    private Empresa empresa;
    private Sede sede;
    private Area area;
    private Tema tema;
    private TipoDocumento tipoDocumentoPerfil;

    private Asesor asesor;
    private Estudiante estudiante;
    private Monitor monitor;

    private Persona principal;
    private Persona parte;
    private Persona contraparte;
    private Persona asesorDirecto;
    private Persona archivada;
    private Persona inactiva;

    @BeforeEach
    void setUp() {
        crearCatalogos();
        crearPerfiles();
        crearPersonas();
        crearConsultas();
        entityManager.flush();
        entityManager.clear();
    }

    // =========================================================
    // Tests históricos — usan alcance GLOBAL para conservar
    // el mismo significado anterior a SCRUM-264
    // =========================================================

    @Test
    void debeBuscarPorNombreApellidoNombreCompletoYDocumento() {
        assertIds(
                buscarGlobal("ana", null, PageRequest.of(0, 10)),
                principal.getId());

        assertIds(
                buscarGlobal("Bermudez", null, PageRequest.of(0, 10)),
                parte.getId());

        assertIds(
                buscarGlobal("Carla Contreras", null, PageRequest.of(0, 10)),
                contraparte.getId());

        assertIds(
                buscarGlobal("1090123456", null, PageRequest.of(0, 10)),
                principal.getId());
    }

    @Test
    void debePaginarContarYOrdenarDeterministicamenteEnBaseDeDatos() {
        Sort sort = Sort.by(
                Sort.Order.asc("nombres").ignoreCase(),
                Sort.Order.asc("id"));

        Page<PersonaResumenProjection> primeraPagina =
                buscarGlobal(null, null, PageRequest.of(0, 2, sort));

        Page<PersonaResumenProjection> segundaPagina =
                buscarGlobal(null, null, PageRequest.of(1, 2, sort));

        assertEquals(6, primeraPagina.getTotalElements());
        assertEquals(3, primeraPagina.getTotalPages());

        assertEquals(
                List.of("Ana", "Bruno"),
                nombres(primeraPagina));

        assertEquals(
                List.of("Carla", "Elena"),
                nombres(segundaPagina));
    }

    @Test
    void debeOrdenarPorCampoAscDescYDesempatarPorId() {
        Persona zoe1 = crearPersona("Zoe", "Zapata", "1090999001", true);
        Persona zoe2 = crearPersona("Zoe", "Alvarez", "1090999002", true);
        entityManager.flush();
        entityManager.clear();

        assertTrue(zoe1.getId() < zoe2.getId());

        // 1. Orden ASC de un campo
        Sort sortAsc = Sort.by(
                Sort.Order.asc("nombres").ignoreCase(),
                Sort.Order.asc("id"));
        Page<PersonaResumenProjection> paginaAsc =
                buscarGlobal(null, null, PageRequest.of(0, 2, sortAsc));

        assertEquals(List.of("Ana", "Bruno"), nombres(paginaAsc));

        // 2. Orden DESC de un campo
        Sort sortDesc = Sort.by(
                Sort.Order.desc("nombres").ignoreCase(),
                Sort.Order.asc("id"));
        Page<PersonaResumenProjection> paginaDesc =
                buscarGlobal(null, null, PageRequest.of(0, 2, sortDesc));

        assertEquals(List.of("Zoe", "Zoe"), nombres(paginaDesc));

        // 3. Desempate estable por id ASC entre dos registros con el mismo valor
        assertEquals(zoe1.getId(), paginaDesc.getContent().get(0).getId());
        assertEquals(zoe2.getId(), paginaDesc.getContent().get(1).getId());
    }

    @Test
    void debeOrdenarPorPropiedadRelacionadaTipoPersonaNombre() {
        TipoPersona tipoApoderado = new TipoPersona();
        tipoApoderado.setNombre("Apoderado");
        entityManager.persist(tipoApoderado);

        Persona apoderado = crearPersona("Zack", "Zuniga", "1090999888", true);
        apoderado.setTipoPersona(tipoApoderado);
        entityManager.persist(apoderado);
        entityManager.flush();
        entityManager.clear();

        Sort sort = Sort.by(
                Sort.Order.asc("tipoPersona.nombre"),
                Sort.Order.asc("id"));

        Page<PersonaResumenProjection> pagina =
                buscarGlobal(null, null, PageRequest.of(0, 10, sort));

        assertEquals("Apoderado", pagina.getContent().getFirst().getTipoPersona());
        assertEquals("Zack", pagina.getContent().getFirst().getNombres());
        assertEquals("Solicitante", pagina.getContent().get(1).getTipoPersona());
    }

    @Test
    void projectionDebeContenerSoloColumnasDelResumen() {
        Page<PersonaResumenProjection> resultado =
                buscarGlobal("1090123456", null, PageRequest.of(0, 10));

        PersonaResumenProjection projection =
                resultado.getContent().getFirst();

        assertEquals(principal.getId(), projection.getId());
        assertEquals("Ana", projection.getNombres());
        assertEquals("CC", projection.getTipoDocumento());
        assertEquals("1090123456", projection.getNumeroDocumento());
        assertEquals("Solicitante", projection.getTipoPersona());
        assertTrue(projection.getActivo());
    }

    @Test
    void activosDebeExcluirPersonasInactivasAntesDeContarYPaginar() {
        Page<PersonaResumenProjection> resultado =
                buscarGlobal(null, true, PageRequest.of(0, 10));

        assertEquals(5, resultado.getTotalElements());

        assertFalse(
                resultado.getContent()
                        .stream()
                        .anyMatch(item ->
                                item.getId().equals(inactiva.getId())));
    }

    // =========================================================
    // Tests históricos de scope via PersonaConsultaScopeRepository
    // =========================================================

    @Test
    void estudianteDebeTenerAlcancePorPrincipalParteYContraparte() {
        assertTrue(existeParaEstudiante(principal));
        assertTrue(existeParaEstudiante(parte));
        assertTrue(existeParaEstudiante(contraparte));

        assertFalse(existeParaEstudiante(asesorDirecto));
    }

    @Test
    void asesorDebeTenerAlcanceDirectoYMedianteEstudianteAsociado() {
        assertTrue(existeParaAsesor(asesorDirecto));
        assertTrue(existeParaAsesor(principal));
        assertTrue(existeParaAsesor(parte));
        assertTrue(existeParaAsesor(contraparte));
    }

    @Test
    void monitorDebeTenerAlcancePorPrincipalParteYContraparte() {
        assertTrue(existeParaMonitor(principal));
        assertTrue(existeParaMonitor(parte));
        assertTrue(existeParaMonitor(contraparte));

        assertFalse(existeParaMonitor(asesorDirecto));
    }

    @Test
    void consultaArchivadaNoDebeOtorgarAlcanceOperativo() {
        assertFalse(existeParaEstudiante(archivada));
        assertFalse(existeParaAsesor(archivada));
        assertFalse(existeParaMonitor(archivada));
    }

    @Test
    void personaInexistenteNoDebeOtorgarAlcance() {
        assertFalse(
                personaConsultaScopeRepository
                        .existsPersonaEnConsultaDeEstudiante(
                                Long.MAX_VALUE,
                                estudiante.getId(),
                                EstadoConsulta.ARCHIVADO));
    }

    // =========================================================
    // Tests nuevos — scope en buscarResumen (listado paginado)
    // =========================================================

    @Test
    void busquedaConScopeEstudianteDebeExcluirPersonasFueraDeAlcance() {
        // Persona fuera del scope del estudiante (sin consulta asociada)
        Persona fueraDeAlcance = crearPersona("Ana", "Externa", "1090777001", true);
        entityManager.flush();
        entityManager.clear();

        // Solo "principal" (nombre "Ana") pertenece al scope del estudiante.
        // "fueraDeAlcance" también se llama "Ana" pero está fuera del scope.
        Page<PersonaResumenProjection> resultado = personaRepository.buscarResumen(
                "ana",
                null,
                false,
                "ESTUDIANTE",
                estudiante.getId(),
                EstadoConsulta.ARCHIVADO,
                PageRequest.of(0, 10));

        assertEquals(1, resultado.getTotalElements());
        assertEquals(principal.getId(), resultado.getContent().getFirst().getId());
        assertFalse(resultado.getContent().stream()
                .anyMatch(p -> p.getId().equals(fueraDeAlcance.getId())));
    }

    @Test
    void activoConScopeEstudianteDebeExcluirPersonaInactivaAunqueEsteEnConsulta() {
        // Persona inactiva que SÍ está en la consulta del estudiante como parte
        Persona parteInactiva = crearPersona("Greta", "Garcia", "1090888001", false);
        entityManager.flush();

        Consulta consultaConInactiva = nuevaConsulta(
                principal, EstadoConsulta.ACTIVO, "Consulta con parte inactiva");
        consultaConInactiva.setEstudiante(estudiante);
        consultaConInactiva.setPartes(new ArrayList<>(List.of(parteInactiva)));
        entityManager.persist(consultaConInactiva);
        entityManager.flush();
        entityManager.clear();

        Page<PersonaResumenProjection> resultado = personaRepository.buscarResumen(
                null,
                true,
                false,
                "ESTUDIANTE",
                estudiante.getId(),
                EstadoConsulta.ARCHIVADO,
                PageRequest.of(0, 20));

        assertFalse(resultado.getContent().stream()
                .anyMatch(p -> p.getId().equals(parteInactiva.getId())));
        assertFalse(resultado.getContent().stream()
                .anyMatch(p -> !p.getActivo()));
    }

    @Test
    void paginacionConScopeEstudianteDebeContarSoloPersonasVisibles() {
        // 3 personas visibles para el estudiante: principal, parte, contraparte
        // Más personas fuera del scope no deben afectar totalElements ni totalPages
        Persona externa1 = crearPersona("Hugo", "Herrera", "1090888100", true);
        Persona externa2 = crearPersona("Irma", "Ibarra", "1090888200", true);
        entityManager.flush();
        entityManager.clear();

        Page<PersonaResumenProjection> resultado = personaRepository.buscarResumen(
                null,
                null,
                false,
                "ESTUDIANTE",
                estudiante.getId(),
                EstadoConsulta.ARCHIVADO,
                PageRequest.of(0, 2));

        assertEquals(2, resultado.getContent().size());
        assertEquals(3, resultado.getTotalElements());
        assertEquals(2, resultado.getTotalPages());

        assertFalse(resultado.getContent().stream()
                .anyMatch(p -> p.getId().equals(externa1.getId())));
        assertFalse(resultado.getContent().stream()
                .anyMatch(p -> p.getId().equals(externa2.getId())));
    }

    @Test
    void scopeEstudianteDebeVerPrincipalParteYContraparteNoArchivadasYNoOtras() {
        // principal, parte, contraparte → visibles
        // asesorDirecto → no está en consulta del estudiante
        // archivada → está en consulta ARCHIVADA del estudiante
        Page<PersonaResumenProjection> resultado = personaRepository.buscarResumen(
                null,
                null,
                false,
                "ESTUDIANTE",
                estudiante.getId(),
                EstadoConsulta.ARCHIVADO,
                PageRequest.of(0, 20));

        List<Long> ids = resultado.getContent().stream()
                .map(PersonaResumenProjection::getId).toList();

        assertTrue(ids.contains(principal.getId()));
        assertTrue(ids.contains(parte.getId()));
        assertTrue(ids.contains(contraparte.getId()));
        assertFalse(ids.contains(asesorDirecto.getId()));
        assertFalse(ids.contains(archivada.getId()));
        assertEquals(3, resultado.getTotalElements());
    }

    @Test
    void scopeAsesorDebeVerPersonasDeConsultaDirectaYDeEstudianteAsociado() {
        // asesorDirecto: principal de consultaAsesorDirecto donde asesor=asesor
        // principal, parte, contraparte: de consultaOperativa donde estudiante.asesor=asesor
        // archivada: NO visible (consulta archivada)
        Page<PersonaResumenProjection> resultado = personaRepository.buscarResumen(
                null,
                null,
                false,
                "ASESOR",
                asesor.getId(),
                EstadoConsulta.ARCHIVADO,
                PageRequest.of(0, 20));

        List<Long> ids = resultado.getContent().stream()
                .map(PersonaResumenProjection::getId).toList();

        assertTrue(ids.contains(asesorDirecto.getId()));
        assertTrue(ids.contains(principal.getId()));
        assertTrue(ids.contains(parte.getId()));
        assertTrue(ids.contains(contraparte.getId()));
        assertFalse(ids.contains(archivada.getId()));
    }

    @Test
    void scopeAsesorDistintoNoDebeVerPersonasDeOtroAsesor() {
        Asesor otroAsesor = new Asesor();
        otroAsesor.setNombre("Otro Asesor");
        otroAsesor.setTipoDocumento(tipoDocumentoPerfil);
        otroAsesor.setDocumento("A-999");
        otroAsesor.setEmail("otro.asesor@example.com");
        otroAsesor.setTelefono("3009999999");
        otroAsesor.setUsuario("otro.asesor");
        otroAsesor.setSede(sede);
        otroAsesor.setCodigo("ASE-999");
        otroAsesor.setArea(area);
        entityManager.persist(otroAsesor);
        entityManager.flush();
        entityManager.clear();

        Page<PersonaResumenProjection> resultado = personaRepository.buscarResumen(
                null,
                null,
                false,
                "ASESOR",
                otroAsesor.getId(),
                EstadoConsulta.ARCHIVADO,
                PageRequest.of(0, 20));

        assertEquals(0, resultado.getTotalElements());
    }

    @Test
    void scopeMonitorDebeVerPersonasDeConsultaNoArchivadasYNoOtros() {
        // consultaOperativa tiene monitor=monitor: principal, parte, contraparte visibles
        // asesorDirecto: NO en consulta del monitor
        // archivada: en consulta archivada del monitor → NO visible
        Page<PersonaResumenProjection> resultado = personaRepository.buscarResumen(
                null,
                null,
                false,
                "MONITOR",
                monitor.getId(),
                EstadoConsulta.ARCHIVADO,
                PageRequest.of(0, 20));

        List<Long> ids = resultado.getContent().stream()
                .map(PersonaResumenProjection::getId).toList();

        assertTrue(ids.contains(principal.getId()));
        assertTrue(ids.contains(parte.getId()));
        assertTrue(ids.contains(contraparte.getId()));
        assertFalse(ids.contains(asesorDirecto.getId()));
        assertFalse(ids.contains(archivada.getId()));
    }

    @Test
    void scopeConciliadorDebeVerPersonasDeConsultaActivaYNoConciliacionInactivaOArchivada() {
        // Fixtures locales para no contaminar históricos
        Conciliador conciliador = crearConciliador("CON-01", "conc01");
        Conciliador otroConciliador = crearConciliador("CON-02", "conc02");

        Persona personaPrincipal = crearPersona("Julia", "Jimenez", "1090CC01", true);
        Persona personaParte = crearPersona("Kevin", "Kline", "1090CC02", true);
        Persona personaContraparte = crearPersona("Laura", "Linares", "1090CC03", true);
        Persona personaOtroConciliador = crearPersona("Marco", "Mora", "1090CC04", true);
        Persona personaConciliacionInactiva = crearPersona("Nora", "Nieto", "1090CC05", true);
        Persona personaConsultaArchivada = crearPersona("Oscar", "Ortiz", "1090CC06", true);

        entityManager.flush();

        // Consulta activa con conciliacion activa para conciliador
        Consulta consultaActiva = nuevaConsulta(personaPrincipal, EstadoConsulta.EN_PROCESO, "Consulta conciliador");
        consultaActiva.setPartes(new ArrayList<>(List.of(personaParte)));
        consultaActiva.setContrapartes(new ArrayList<>(List.of(personaContraparte)));
        entityManager.persist(consultaActiva);
        entityManager.flush();

        Conciliacion conciliacionActiva = crearConciliacion(consultaActiva, conciliador, true);

        // Mismo conciliador pero conciliacion inactiva
        Consulta consultaConInactiva = nuevaConsulta(personaConciliacionInactiva, EstadoConsulta.EN_PROCESO, "Conciliacion inactiva");
        entityManager.persist(consultaConInactiva);
        entityManager.flush();

        Conciliacion conciliacionInactiva = crearConciliacion(consultaConInactiva, conciliador, false);

        // Consulta ARCHIVADA con conciliacion activa del conciliador
        Consulta consultaArchivadaConciliador = nuevaConsulta(personaConsultaArchivada, EstadoConsulta.ARCHIVADO, "Consulta archivada conciliador");
        entityManager.persist(consultaArchivadaConciliador);
        entityManager.flush();

        Conciliacion conciliacionDeArchivada = crearConciliacion(consultaArchivadaConciliador, conciliador, true);

        // Otro conciliador
        Consulta consultaOtroConciliador = nuevaConsulta(personaOtroConciliador, EstadoConsulta.ACTIVO, "Otro conciliador");
        entityManager.persist(consultaOtroConciliador);
        entityManager.flush();

        Conciliacion conciliacionOtro = crearConciliacion(consultaOtroConciliador, otroConciliador, true);

        entityManager.flush();
        entityManager.clear();

        Page<PersonaResumenProjection> resultado = personaRepository.buscarResumen(
                null,
                null,
                false,
                "CONCILIADOR",
                conciliador.getId(),
                EstadoConsulta.ARCHIVADO,
                PageRequest.of(0, 20));

        List<Long> ids = resultado.getContent().stream()
                .map(PersonaResumenProjection::getId).toList();

        // Visibles: principal, parte y contraparte de conciliacion activa + consulta no archivada
        assertTrue(ids.contains(personaPrincipal.getId()), "principal debe ser visible");
        assertTrue(ids.contains(personaParte.getId()), "parte debe ser visible");
        assertTrue(ids.contains(personaContraparte.getId()), "contraparte debe ser visible");

        // No visibles
        assertFalse(ids.contains(personaOtroConciliador.getId()), "otro conciliador no debe ser visible");
        assertFalse(ids.contains(personaConciliacionInactiva.getId()), "conciliacion inactiva no debe ser visible");
        assertFalse(ids.contains(personaConsultaArchivada.getId()), "consulta archivada no debe ser visible");

        assertEquals(3, resultado.getTotalElements());
    }

    @Test
    void conciliadorDebeVerPersonaPrincipalParteYContrapartePorScopeRepository() {
        Conciliador conciliador = crearConciliador("CON-SC01", "concsc01");

        Persona personaPrincipal = crearPersona("Rosa", "Rivas", "1090SC01", true);
        Persona personaParte = crearPersona("Samuel", "Sosa", "1090SC02", true);
        Persona personaContraparte = crearPersona("Teresa", "Torres", "1090SC03", true);
        Persona personaOtroConciliador = crearPersona("Ulises", "Uribe", "1090SC04", true);
        Persona personaConciliacionInactiva = crearPersona("Vera", "Vega", "1090SC05", true);
        Persona personaConsultaArchivada = crearPersona("Walter", "Wolff", "1090SC06", true);

        entityManager.flush();

        Consulta consultaActiva = nuevaConsulta(personaPrincipal, EstadoConsulta.ACTIVO, "Scope conciliador principal");
        consultaActiva.setPartes(new ArrayList<>(List.of(personaParte)));
        consultaActiva.setContrapartes(new ArrayList<>(List.of(personaContraparte)));
        entityManager.persist(consultaActiva);
        entityManager.flush();

        Conciliacion conciliacionActiva = crearConciliacion(consultaActiva, conciliador, true);

        Conciliador otroConciliador = crearConciliador("CON-SC02", "concsc02");
        Consulta consultaOtra = nuevaConsulta(personaOtroConciliador, EstadoConsulta.ACTIVO, "Otro conciliador scope");
        entityManager.persist(consultaOtra);
        entityManager.flush();

        Conciliacion conciliacionOtra = crearConciliacion(consultaOtra, otroConciliador, true);

        Consulta consultaInactiva = nuevaConsulta(personaConciliacionInactiva, EstadoConsulta.ACTIVO, "Conciliacion inactiva scope");
        entityManager.persist(consultaInactiva);
        entityManager.flush();

        Conciliacion conciliacionInactiva = crearConciliacion(consultaInactiva, conciliador, false);

        Consulta consultaArchivadaConciliador = nuevaConsulta(personaConsultaArchivada, EstadoConsulta.ARCHIVADO, "Archivada scope conciliador");
        entityManager.persist(consultaArchivadaConciliador);
        entityManager.flush();

        Conciliacion conciliacionArchivada = crearConciliacion(consultaArchivadaConciliador, conciliador, true);

        entityManager.flush();
        entityManager.clear();

        // Persona principal → true
        assertTrue(personaConsultaScopeRepository.existsPersonaEnConciliacionDeConciliador(
                personaPrincipal.getId(), conciliador.getId(), EstadoConsulta.ARCHIVADO),
                "principal debe ser visible");

        // Persona parte → true
        assertTrue(personaConsultaScopeRepository.existsPersonaEnConciliacionDeConciliador(
                personaParte.getId(), conciliador.getId(), EstadoConsulta.ARCHIVADO),
                "parte debe ser visible");

        // Persona contraparte → true
        assertTrue(personaConsultaScopeRepository.existsPersonaEnConciliacionDeConciliador(
                personaContraparte.getId(), conciliador.getId(), EstadoConsulta.ARCHIVADO),
                "contraparte debe ser visible");

        // Otro conciliador → false
        assertFalse(personaConsultaScopeRepository.existsPersonaEnConciliacionDeConciliador(
                personaOtroConciliador.getId(), conciliador.getId(), EstadoConsulta.ARCHIVADO),
                "otro conciliador no debe ser visible");

        // Conciliacion inactiva → false
        assertFalse(personaConsultaScopeRepository.existsPersonaEnConciliacionDeConciliador(
                personaConciliacionInactiva.getId(), conciliador.getId(), EstadoConsulta.ARCHIVADO),
                "conciliacion inactiva no debe ser visible");

        // Consulta archivada → false
        assertFalse(personaConsultaScopeRepository.existsPersonaEnConciliacionDeConciliador(
                personaConsultaArchivada.getId(), conciliador.getId(), EstadoConsulta.ARCHIVADO),
                "consulta archivada no debe ser visible");
    }

    // =========================================================
    // Helpers de fixtures y assertions
    // =========================================================

    private Page<PersonaResumenProjection> buscarGlobal(
            String search, Boolean activo, PageRequest pageable) {
        return personaRepository.buscarResumen(
                search, activo, true, null, null, EstadoConsulta.ARCHIVADO, pageable);
    }

    private EstadoConciliacion obtenerEstadoConciliacion() {
        java.util.List<EstadoConciliacion> list = entityManager
                .createQuery("SELECT e FROM EstadoConciliacion e", EstadoConciliacion.class)
                .getResultList();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        EstadoConciliacion estado = new EstadoConciliacion();
        estado.setCodigo("EST_TRAMITE_TEST");
        estado.setNombre("En Tramite Test");
        entityManager.persist(estado);
        return estado;
    }

    private UsuarioSistema obtenerUsuarioSistema() {
        java.util.List<UsuarioSistema> list = entityManager
                .createQuery("SELECT u FROM UsuarioSistema u", UsuarioSistema.class)
                .getResultList();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        Rol rol = new Rol();
        rol.setNombre("ROL_SISTEMA_TEST");
        rol.setTipoPerfil(TipoPerfilUsuario.ADMINISTRATIVO);
        entityManager.persist(rol);

        UsuarioSistema usuario = new UsuarioSistema();
        usuario.setUsername("sistema_test@example.com");
        usuario.setPasswordHash("hash");
        usuario.setRol(rol);
        entityManager.persist(usuario);
        return usuario;
    }

    private Conciliacion crearConciliacion(Consulta consulta, Conciliador conciliador, boolean activo) {
        Conciliacion conciliacion = new Conciliacion();
        conciliacion.setConsulta(consulta);
        conciliacion.setConciliador(conciliador);
        conciliacion.setEstado(obtenerEstadoConciliacion());
        conciliacion.setSolicitadoPor(obtenerUsuarioSistema());
        conciliacion.setActivo(activo);
        entityManager.persist(conciliacion);
        return conciliacion;
    }

    private Conciliador crearConciliador(String codigo, String usuario) {
        Conciliador conciliador = new Conciliador();
        conciliador.setNombre("Conciliador " + codigo);
        conciliador.setTipoDocumento(tipoDocumentoPerfil);
        conciliador.setDocumento("C-" + codigo);
        conciliador.setEmail(usuario + "@example.com");
        conciliador.setTelefono("300-" + codigo);
        conciliador.setUsuario(usuario);
        conciliador.setSede(sede);
        conciliador.setCodigo(codigo);
        conciliador.setTipoConciliador(TipoConciliador.INTERNO);
        entityManager.persist(conciliador);
        return conciliador;
    }

    private void crearCatalogos() {
        Departamento departamento = new Departamento();
        departamento.setNombre("Norte de Santander");
        entityManager.persist(departamento);

        municipio = new Municipio();
        municipio.setNombre("Cucuta");
        municipio.setDepartamento(departamento);
        entityManager.persist(municipio);

        barrio = new Barrio();
        barrio.setNombre("Centro");
        barrio.setMunicipio(municipio);
        entityManager.persist(barrio);

        nacionalidad = new Nacionalidad();
        nacionalidad.setNombre("Colombiana");
        entityManager.persist(nacionalidad);

        tipoPersona = new TipoPersona();
        tipoPersona.setNombre("Solicitante");
        entityManager.persist(tipoPersona);

        condicion = new Condicion();
        condicion.setNombre("Ninguna");
        entityManager.persist(condicion);

        ocupacion = new Ocupacion();
        ocupacion.setNombre("Independiente");
        entityManager.persist(ocupacion);

        empresa = new Empresa();
        empresa.setNombre("No aplica");
        entityManager.persist(empresa);

        sede = new Sede();
        sede.setNombre("Principal");
        entityManager.persist(sede);

        area = new Area();
        area.setNombre("Civil");
        entityManager.persist(area);

        tema = new Tema();
        tema.setNombre("Contratos");
        tema.setArea(area);
        entityManager.persist(tema);

        tipoDocumentoPerfil = new TipoDocumento();
        tipoDocumentoPerfil.setNombre("Cedula de ciudadania");
        entityManager.persist(tipoDocumentoPerfil);
    }

    private void crearPerfiles() {
        asesor = new Asesor();
        asesor.setNombre("Asesor SEC-07");
        asesor.setTipoDocumento(tipoDocumentoPerfil);
        asesor.setDocumento("A-100");
        asesor.setEmail("asesor.sec07@example.com");
        asesor.setTelefono("3000000100");
        asesor.setUsuario("asesor.sec07");
        asesor.setSede(sede);
        asesor.setCodigo("ASE-100");
        asesor.setArea(area);
        entityManager.persist(asesor);

        estudiante = new Estudiante();
        estudiante.setNombre("Estudiante SEC-07");
        estudiante.setTipoDocumento(tipoDocumentoPerfil);
        estudiante.setDocumento("E-100");
        estudiante.setEmail("estudiante.sec07@example.com");
        estudiante.setTelefono("3000000200");
        estudiante.setUsuario("estudiante.sec07");
        estudiante.setSede(sede);
        estudiante.setCodigo("EST-100");
        estudiante.setAsesor(asesor);
        entityManager.persist(estudiante);

        monitor = new Monitor();
        monitor.setNombre("Monitor SEC-07");
        monitor.setTipoDocumento(tipoDocumentoPerfil);
        monitor.setDocumento("M-100");
        monitor.setEmail("monitor.sec07@example.com");
        monitor.setTelefono("3000000300");
        monitor.setUsuario("monitor.sec07");
        monitor.setCodigo("MON-100");
        monitor.setSede(sede);
        entityManager.persist(monitor);
    }

    private void crearPersonas() {
        principal = crearPersona(
                "Ana",
                "Alvarez",
                "1090123456",
                true);

        parte = crearPersona(
                "Bruno",
                "Bermudez",
                "1090222222",
                true);

        contraparte = crearPersona(
                "Carla",
                "Contreras",
                "1090333333",
                true);

        asesorDirecto = crearPersona(
                "Lucia",
                "Diaz",
                "1090444444",
                true);

        archivada = crearPersona(
                "Elena",
                "Escalante",
                "1090555555",
                true);

        inactiva = crearPersona(
                "Fabio",
                "Fuentes",
                "1090666666",
                false);
    }

    private Persona crearPersona(
            String nombres,
            String apellidos,
            String documento,
            boolean activo) {

        Persona persona = new Persona();

        persona.setTipoPersona(tipoPersona);
        persona.setTipoDocumento("CC");
        persona.setNumeroDocumento(documento);
        persona.setFechaExpedicion(LocalDate.of(2010, 1, 1));
        persona.setCiudadExpedicion("Cucuta");

        persona.setNombres(nombres);
        persona.setApellidos(apellidos);
        persona.setNombreIdentitario(nombres);
        persona.setPronombre("ella");

        persona.setSexo("mujer");
        persona.setGenero("femenino");
        persona.setOrientacionSexual("no informa");
        persona.setFechaNacimiento(LocalDate.of(1990, 1, 1));

        persona.setTelefono("3001234567");
        persona.setCorreo(documento + "@example.com");

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
        persona.setDireccion("Direccion de prueba");
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

        persona.setActivo(activo);

        entityManager.persist(persona);

        return persona;
    }

    private void crearConsultas() {
        Consulta consultaOperativa = nuevaConsulta(
                principal,
                EstadoConsulta.ACTIVO,
                "Consulta operativa");

        consultaOperativa.setEstudiante(estudiante);
        consultaOperativa.setMonitor(monitor);

        consultaOperativa.setPartes(
                new ArrayList<>(List.of(parte)));

        consultaOperativa.setContrapartes(
                new ArrayList<>(List.of(contraparte)));

        entityManager.persist(consultaOperativa);

        Consulta consultaAsesorDirecto = nuevaConsulta(
                asesorDirecto,
                EstadoConsulta.EN_PROCESO,
                "Consulta asesor directo");

        consultaAsesorDirecto.setAsesor(asesor);

        entityManager.persist(consultaAsesorDirecto);

        Consulta consultaArchivada = nuevaConsulta(
                archivada,
                EstadoConsulta.ARCHIVADO,
                "Consulta archivada");

        consultaArchivada.setEstudiante(estudiante);
        consultaArchivada.setAsesor(asesor);
        consultaArchivada.setMonitor(monitor);

        entityManager.persist(consultaArchivada);
    }

    private Consulta nuevaConsulta(
            Persona persona,
            EstadoConsulta estado,
            String descripcion) {

        Consulta consulta = new Consulta();

        consulta.setFecha(LocalDate.now());
        consulta.setDescripcion(descripcion);
        consulta.setHechos("Hechos de prueba");
        consulta.setPretensiones("Pretensiones de prueba");
        consulta.setConceptoJuridico("Concepto de prueba");
        consulta.setTramite("Asesoria");

        consulta.setEstado(estado);
        consulta.setPersona(persona);

        consulta.setSede(sede);
        consulta.setArea(area);
        consulta.setTema(tema);

        return consulta;
    }

    private boolean existeParaEstudiante(Persona persona) {
        return personaConsultaScopeRepository
                .existsPersonaEnConsultaDeEstudiante(
                        persona.getId(),
                        estudiante.getId(),
                        EstadoConsulta.ARCHIVADO);
    }

    private boolean existeParaAsesor(Persona persona) {
        return personaConsultaScopeRepository
                .existsPersonaEnConsultaDeAsesor(
                        persona.getId(),
                        asesor.getId(),
                        EstadoConsulta.ARCHIVADO);
    }

    private boolean existeParaMonitor(Persona persona) {
        return personaConsultaScopeRepository
                .existsPersonaEnConsultaDeMonitor(
                        persona.getId(),
                        monitor.getId(),
                        EstadoConsulta.ARCHIVADO);
    }

    private void assertIds(
            Page<PersonaResumenProjection> page,
            Long... ids) {

        assertEquals(
                List.of(ids),
                page.getContent()
                        .stream()
                        .map(PersonaResumenProjection::getId)
                        .toList());
    }

    private List<String> nombres(
            Page<PersonaResumenProjection> page) {

        return page.getContent()
                .stream()
                .map(PersonaResumenProjection::getNombres)
                .toList();
    }
}
