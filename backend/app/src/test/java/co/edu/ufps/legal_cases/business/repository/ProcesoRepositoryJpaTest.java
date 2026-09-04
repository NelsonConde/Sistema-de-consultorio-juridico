package co.edu.ufps.legal_cases.business.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
import co.edu.ufps.legal_cases.business.model.catalogo.Tipo;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.model.consulta.Consulta;
import co.edu.ufps.legal_cases.business.model.consulta.EstadoConsulta;
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.model.persona.Condicion;
import co.edu.ufps.legal_cases.business.model.persona.Empresa;
import co.edu.ufps.legal_cases.business.model.persona.Ocupacion;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.model.persona.TipoPersona;
import co.edu.ufps.legal_cases.business.model.proceso.Especialidad;
import co.edu.ufps.legal_cases.business.model.proceso.EstadoProceso;
import co.edu.ufps.legal_cases.business.model.proceso.OrganoControl;
import co.edu.ufps.legal_cases.business.model.proceso.Proceso;
import co.edu.ufps.legal_cases.business.repository.proceso.ProcesoRepository;
import co.edu.ufps.legal_cases.business.repository.proceso.ProcesoResumenProjection;
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
class ProcesoRepositoryJpaTest extends PostgreSqlIntegrationTest {

    @Autowired
    private ProcesoRepository procesoRepository;

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
    private Tipo tipo;
    private TipoDocumento tipoDocumentoPerfil;
    private Departamento departamentoNorte;
    private Departamento departamentoSur;
    private OrganoControl organoCivil;
    private OrganoControl organoPenal;
    private Especialidad especialidadFamilia;
    private Especialidad especialidadPenal;
    private Asesor asesorA;
    private Asesor asesorB;
    private Estudiante estudianteA;
    private Estudiante estudianteB;
    private Monitor monitorA;
    private Monitor monitorB;

    private Proceso procesoAlpha;
    private Proceso procesoBeta;
    private Proceso procesoAsesorDirecto;
    private Proceso procesoAsesorPorEstudiante;
    private Proceso procesoMonitorA;
    private Proceso procesoArchivado;
    private Proceso procesoInactivo;

    @BeforeEach
    void setUp() {
        crearCatalogos();
        crearPerfiles();
        crearProcesosBase();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void paginacionGlobalDebeExcluirConsultasArchivadasYProcesosInactivos() {
        Page<ProcesoResumenProjection> resultado = buscarGlobal(
                null,
                null,
                null,
                null,
                PageRequest.of(0, 2, sortIdDesc()));

        assertEquals(2, resultado.getContent().size());
        assertEquals(5, resultado.getTotalElements());
        assertEquals(3, resultado.getTotalPages());
        assertTrue(ids(resultado).stream().noneMatch(procesoArchivado.getId()::equals));
        assertTrue(ids(resultado).stream().noneMatch(procesoInactivo.getId()::equals));
    }

    @Test
    void paginaFueraDeRangoDebeRetornarContentVacioConTotalReal() {
        Page<ProcesoResumenProjection> resultado = buscarGlobal(
                null,
                null,
                null,
                null,
                PageRequest.of(10, 2, sortIdDesc()));

        assertTrue(resultado.getContent().isEmpty());
        assertEquals(5, resultado.getTotalElements());
        assertEquals(3, resultado.getTotalPages());
    }

    @Test
    void searchDebeAplicarseAntesDePaginarYContarSoloCoincidencias() {
        crearProceso(
                crearConsulta(crearPersona("Sara", "Search", "DOC-S-001"),
                        EstadoConsulta.ACTIVO, asesorA, estudianteA, monitorA,
                        "consulta claveproceso por descripcion", LocalDate.of(2026, 2, 1)),
                departamentoNorte,
                organoCivil,
                especialidadFamilia,
                EstadoProceso.PENDIENTE,
                "RAD-SEARCH-001",
                true,
                LocalDateTime.of(2026, 3, 1, 8, 0));
        crearProceso(
                crearConsulta(crearPersona("Tomas", "Search", "DOC-S-002"),
                        EstadoConsulta.ACTIVO, asesorA, estudianteA, monitorA,
                        "consulta comun", LocalDate.of(2026, 2, 2)),
                departamentoSur,
                organoCivil,
                especialidadFamilia,
                EstadoProceso.PENDIENTE,
                "CLAVEPROCESO-002",
                true,
                LocalDateTime.of(2026, 3, 2, 8, 0));
        Especialidad especialidadClave = crearEspecialidad("Claveproceso Especialidad", organoCivil);
        crearProceso(
                crearConsulta(crearPersona("Uma", "Search", "DOC-S-003"),
                        EstadoConsulta.ACTIVO, asesorA, estudianteA, monitorA,
                        "consulta comun", LocalDate.of(2026, 2, 3)),
                departamentoSur,
                organoCivil,
                especialidadClave,
                EstadoProceso.PENDIENTE,
                "RAD-SEARCH-003",
                true,
                LocalDateTime.of(2026, 3, 3, 8, 0));
        crearProceso(
                crearConsulta(crearPersona("Victor", "Search", "DOC-S-004"),
                        EstadoConsulta.ACTIVO, asesorA, estudianteA, monitorA,
                        "consulta sin coincidencia", LocalDate.of(2026, 2, 4)),
                departamentoSur,
                organoPenal,
                especialidadPenal,
                EstadoProceso.PENDIENTE,
                "RAD-SEARCH-004",
                true,
                LocalDateTime.of(2026, 3, 4, 8, 0));
        entityManager.flush();
        entityManager.clear();

        Page<ProcesoResumenProjection> resultado = buscarGlobal(
                "claveproceso",
                null,
                null,
                null,
                PageRequest.of(0, 1, sortIdDesc()));

        assertEquals(1, resultado.getContent().size());
        assertEquals(3, resultado.getTotalElements());
        assertEquals(3, resultado.getTotalPages());
    }

    @Test
    void filtroEstadoDebeAplicarseEnContentYCount() {
        Page<ProcesoResumenProjection> resultado = buscarGlobal(
                null,
                EstadoProceso.SENTENCIA_FAVORABLE,
                null,
                null,
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, procesoAsesorDirecto.getId(), procesoBeta.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void rangoDeFechasDebeAplicarseEnContentYCount() {
        Page<ProcesoResumenProjection> resultado = buscarGlobal(
                null,
                null,
                LocalDate.of(2026, 1, 12),
                LocalDate.of(2026, 1, 13),
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, procesoMonitorA.getId(), procesoAsesorPorEstudiante.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void filtrosCombinadosDebenAplicarseConAnd() {
        Page<ProcesoResumenProjection> resultado = buscarGlobal(
                "penal",
                EstadoProceso.SENTENCIA_DESFAVORABLE,
                LocalDate.of(2026, 1, 13),
                LocalDate.of(2026, 1, 13),
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, procesoMonitorA.getId());
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void scopeEstudianteDebeVerSoloProcesosDeSusConsultas() {
        Page<ProcesoResumenProjection> resultado = buscarRestringido(
                "ESTUDIANTE",
                estudianteA.getId(),
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, procesoAsesorPorEstudiante.getId(), procesoAlpha.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void scopeAsesorDebeConservarRamaDirectaYPorEstudiante() {
        Page<ProcesoResumenProjection> resultado = buscarRestringido(
                "ASESOR",
                asesorA.getId(),
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(
                resultado,
                procesoAsesorPorEstudiante.getId(),
                procesoAsesorDirecto.getId(),
                procesoAlpha.getId());
        assertEquals(3, resultado.getTotalElements());
    }

    @Test
    void scopeMonitorDebeVerSoloProcesosDeConsultasAsignadas() {
        Page<ProcesoResumenProjection> resultado = buscarRestringido(
                "MONITOR",
                monitorA.getId(),
                PageRequest.of(0, 10, sortIdDesc()));

        assertIds(resultado, procesoMonitorA.getId(), procesoAlpha.getId());
        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void conciliadorYPerfilNoSoportadoDebenResolverFailClosed() {
        Page<ProcesoResumenProjection> conciliador = buscarRestringido(
                "CONCILIADOR",
                999L,
                PageRequest.of(0, 10, sortIdDesc()));
        Page<ProcesoResumenProjection> administrativoNoGlobal = buscarRestringido(
                "ADMINISTRATIVO",
                999L,
                PageRequest.of(0, 10, sortIdDesc()));

        assertTrue(conciliador.getContent().isEmpty());
        assertEquals(0, conciliador.getTotalElements());
        assertTrue(administrativoNoGlobal.getContent().isEmpty());
        assertEquals(0, administrativoNoGlobal.getTotalElements());
    }

    @Test
    void ordenPorFechaCreacionDebeUsarIdAscComoDesempate() {
        Page<ProcesoResumenProjection> resultado = buscarGlobal(
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10, Sort.by(
                        Sort.Order.asc("fechaCreacion"),
                        Sort.Order.asc("id"))));

        List<Long> ids = ids(resultado);

        assertTrue(ids.indexOf(procesoAlpha.getId()) < ids.indexOf(procesoAsesorDirecto.getId()));
    }

    @Test
    void proyeccionResumenDebeExponerCamposEsperados() {
        Page<ProcesoResumenProjection> resultado = buscarGlobal(
                "alpha",
                null,
                null,
                null,
                PageRequest.of(0, 10, sortIdDesc()));

        ProcesoResumenProjection projection = resultado.getContent().getFirst();

        assertEquals(procesoAlpha.getId(), projection.getId());
        assertNotNull(projection.getVersion());
        assertEquals("RAD-ALPHA-001", projection.getNumeroRadicado());
        assertEquals(departamentoNorte.getId(), projection.getDepartamentoId());
        assertEquals("Departamento Norte", projection.getDepartamentoNombre());
        assertEquals("consulta alpha", projection.getConsulta());
        assertEquals(organoCivil.getId(), projection.getOrganoControlId());
        assertEquals("Organo Civil", projection.getOrganoControlNombre());
        assertEquals(especialidadFamilia.getId(), projection.getEspecialidadId());
        assertEquals("Familia", projection.getEspecialidadNombre());
        assertEquals(EstadoProceso.PENDIENTE, projection.getEstado());
        assertEquals(true, projection.getActivo());
        assertEquals(LocalDateTime.of(2026, 1, 10, 9, 0), projection.getFechaCreacion());
    }

    @Test
    void accesoAGettersDeProjectionNoDebeGenerarNMasUno() {
        SessionFactory sessionFactory = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);

        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        Page<ProcesoResumenProjection> resultado = buscarGlobal(
                null,
                null,
                null,
                null,
                PageRequest.of(0, 2, sortIdDesc()));

        resultado.getContent().forEach(item -> {
            item.getId();
            item.getVersion();
            item.getNumeroRadicado();
            item.getDepartamentoId();
            item.getDepartamentoNombre();
            item.getConsultaId();
            item.getConsulta();
            item.getOrganoControlId();
            item.getOrganoControlNombre();
            item.getEspecialidadId();
            item.getEspecialidadNombre();
            item.getEstado();
            item.getActivo();
            item.getFechaCreacion();
        });

        assertEquals(2, statistics.getPrepareStatementCount());
    }

    private Page<ProcesoResumenProjection> buscarGlobal(
            String search,
            EstadoProceso estado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            PageRequest pageable) {
        return procesoRepository.buscarResumenPaginado(
                search,
                estado,
                fechaDesde != null ? fechaDesde.atStartOfDay() : null,
                fechaHasta != null ? fechaHasta.plusDays(1).atStartOfDay() : null,
                true,
                null,
                null,
                EstadoConsulta.ARCHIVADO,
                pageable);
    }

    private Page<ProcesoResumenProjection> buscarRestringido(
            String tipoPerfil,
            Long perfilId,
            PageRequest pageable) {
        return procesoRepository.buscarResumenPaginado(
                null,
                null,
                null,
                null,
                false,
                tipoPerfil,
                perfilId,
                EstadoConsulta.ARCHIVADO,
                pageable);
    }

    private Sort sortIdDesc() {
        return Sort.by(Sort.Order.desc("id"));
    }

    private List<Long> ids(Page<ProcesoResumenProjection> page) {
        return page.getContent().stream()
                .map(ProcesoResumenProjection::getId)
                .toList();
    }

    private void assertIds(Page<ProcesoResumenProjection> page, Long... expectedIds) {
        assertEquals(List.of(expectedIds), ids(page));
    }

    private void crearCatalogos() {
        departamentoNorte = new Departamento();
        departamentoNorte.setNombre("Departamento Norte");
        entityManager.persist(departamentoNorte);

        departamentoSur = new Departamento();
        departamentoSur.setNombre("Departamento Sur");
        entityManager.persist(departamentoSur);

        municipio = new Municipio();
        municipio.setNombre("Cucuta");
        municipio.setDepartamento(departamentoNorte);
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

        tipo = new Tipo();
        tipo.setNombre("Asesoria civil");
        tipo.setTema(tema);
        entityManager.persist(tipo);

        tipoDocumentoPerfil = new TipoDocumento();
        tipoDocumentoPerfil.setNombre("Cedula de ciudadania");
        entityManager.persist(tipoDocumentoPerfil);

        organoCivil = crearOrganoControl("Organo Civil");
        organoPenal = crearOrganoControl("Organo Penal");
        especialidadFamilia = crearEspecialidad("Familia", organoCivil);
        especialidadPenal = crearEspecialidad("Penal", organoPenal);
    }

    private OrganoControl crearOrganoControl(String nombre) {
        OrganoControl organoControl = new OrganoControl();
        organoControl.setNombre(nombre);
        organoControl.setActivo(true);
        entityManager.persist(organoControl);
        return organoControl;
    }

    private Especialidad crearEspecialidad(String nombre, OrganoControl organoControl) {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre(nombre);
        especialidad.setOrganoControl(organoControl);
        especialidad.setActivo(true);
        entityManager.persist(especialidad);
        return especialidad;
    }

    private void crearPerfiles() {
        asesorA = crearAsesor("Asesor A", "ASE-A", "A-001", "asesor.a");
        asesorB = crearAsesor("Asesor B", "ASE-B", "A-002", "asesor.b");
        estudianteA = crearEstudiante("Estudiante A", "EST-A", "E-001", "estudiante.a", asesorA);
        estudianteB = crearEstudiante("Estudiante B", "EST-B", "E-002", "estudiante.b", asesorB);
        monitorA = crearMonitor("Monitor A", "MON-A", "M-001", "monitor.a");
        monitorB = crearMonitor("Monitor B", "MON-B", "M-002", "monitor.b");
    }

    private Asesor crearAsesor(String nombre, String codigo, String documento, String usuario) {
        Asesor asesor = new Asesor();
        asesor.setNombre(nombre);
        asesor.setTipoDocumento(tipoDocumentoPerfil);
        asesor.setDocumento(documento);
        asesor.setEmail(usuario + "@example.test");
        asesor.setTelefono("300-" + documento);
        asesor.setUsuario(usuario);
        asesor.setSede(sede);
        asesor.setCodigo(codigo);
        asesor.setArea(area);
        entityManager.persist(asesor);
        return asesor;
    }

    private Estudiante crearEstudiante(
            String nombre,
            String codigo,
            String documento,
            String usuario,
            Asesor asesor) {
        Estudiante estudiante = new Estudiante();
        estudiante.setNombre(nombre);
        estudiante.setTipoDocumento(tipoDocumentoPerfil);
        estudiante.setDocumento(documento);
        estudiante.setEmail(usuario + "@example.test");
        estudiante.setTelefono("310-" + documento);
        estudiante.setUsuario(usuario);
        estudiante.setSede(sede);
        estudiante.setCodigo(codigo);
        estudiante.setAsesor(asesor);
        entityManager.persist(estudiante);
        return estudiante;
    }

    private Monitor crearMonitor(String nombre, String codigo, String documento, String usuario) {
        Monitor monitor = new Monitor();
        monitor.setNombre(nombre);
        monitor.setTipoDocumento(tipoDocumentoPerfil);
        monitor.setDocumento(documento);
        monitor.setEmail(usuario + "@example.test");
        monitor.setTelefono("320-" + documento);
        monitor.setUsuario(usuario);
        monitor.setCodigo(codigo);
        monitor.setSede(sede);
        entityManager.persist(monitor);
        return monitor;
    }

    private void crearProcesosBase() {
        procesoAlpha = crearProceso(
                crearConsulta(crearPersona("Ana", "Alvarez", "DOC-001"),
                        EstadoConsulta.ACTIVO, asesorA, estudianteA, monitorA,
                        "consulta alpha", LocalDate.of(2026, 1, 10)),
                departamentoNorte,
                organoCivil,
                especialidadFamilia,
                EstadoProceso.PENDIENTE,
                "RAD-ALPHA-001",
                true,
                LocalDateTime.of(2026, 1, 10, 9, 0));

        procesoBeta = crearProceso(
                crearConsulta(crearPersona("Bruno", "Bermudez", "DOC-002"),
                        EstadoConsulta.ACTIVO, asesorB, estudianteB, monitorB,
                        "consulta beta", LocalDate.of(2026, 1, 11)),
                departamentoSur,
                organoPenal,
                especialidadPenal,
                EstadoProceso.SENTENCIA_FAVORABLE,
                "RAD-BETA-002",
                true,
                LocalDateTime.of(2026, 1, 11, 9, 0));

        procesoAsesorDirecto = crearProceso(
                crearConsulta(crearPersona("Carla", "Castillo", "DOC-003"),
                        EstadoConsulta.ACTIVO, asesorA, null, monitorB,
                        "consulta directa asesor", LocalDate.of(2026, 1, 10)),
                departamentoNorte,
                organoCivil,
                especialidadFamilia,
                EstadoProceso.SENTENCIA_FAVORABLE,
                "RAD-DIRECT-003",
                true,
                LocalDateTime.of(2026, 1, 10, 15, 0));

        procesoAsesorPorEstudiante = crearProceso(
                crearConsulta(crearPersona("Diana", "Duarte", "DOC-004"),
                        EstadoConsulta.ACTIVO, asesorB, estudianteA, monitorB,
                        "consulta estudiante asesorado", LocalDate.of(2026, 1, 12)),
                departamentoNorte,
                organoCivil,
                especialidadFamilia,
                EstadoProceso.DESISTIMIENTO,
                "RAD-STUDENT-004",
                true,
                LocalDateTime.of(2026, 1, 12, 9, 0));

        procesoMonitorA = crearProceso(
                crearConsulta(crearPersona("Elena", "Escalante", "DOC-005"),
                        EstadoConsulta.ACTIVO, asesorB, estudianteB, monitorA,
                        "consulta monitor penal", LocalDate.of(2026, 1, 13)),
                departamentoSur,
                organoPenal,
                especialidadPenal,
                EstadoProceso.SENTENCIA_DESFAVORABLE,
                "RAD-MONITOR-005",
                true,
                LocalDateTime.of(2026, 1, 13, 23, 59));

        procesoArchivado = crearProceso(
                crearConsulta(crearPersona("Fabio", "Fuentes", "DOC-006"),
                        EstadoConsulta.ARCHIVADO, asesorA, estudianteA, monitorA,
                        "consulta archivada", LocalDate.of(2026, 1, 14)),
                departamentoNorte,
                organoCivil,
                especialidadFamilia,
                EstadoProceso.PENDIENTE,
                "RAD-ARCHIVED-006",
                true,
                LocalDateTime.of(2026, 1, 14, 9, 0));

        procesoInactivo = crearProceso(
                crearConsulta(crearPersona("Gloria", "Garcia", "DOC-007"),
                        EstadoConsulta.ACTIVO, asesorA, estudianteA, monitorA,
                        "consulta proceso inactivo", LocalDate.of(2026, 1, 15)),
                departamentoNorte,
                organoCivil,
                especialidadFamilia,
                EstadoProceso.PENDIENTE,
                "RAD-INACTIVE-007",
                false,
                LocalDateTime.of(2026, 1, 15, 9, 0));
    }

    private Persona crearPersona(String nombres, String apellidos, String documento) {
        Persona persona = new Persona();
        persona.setTipoPersona(tipoPersona);
        persona.setTipoDocumento("CC");
        persona.setNumeroDocumento(documento);
        persona.setFechaExpedicion(LocalDate.of(2010, 1, 1));
        persona.setCiudadExpedicion("Cucuta");
        persona.setNombres(nombres);
        persona.setApellidos(apellidos);
        persona.setNombreIdentitario(nombres);
        persona.setPronombre("no informa");
        persona.setSexo("no informa");
        persona.setGenero("no informa");
        persona.setOrientacionSexual("no informa");
        persona.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        persona.setTelefono("3001234567");
        persona.setCorreo(documento + "@example.test");
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
        persona.setActivo(true);
        entityManager.persist(persona);
        return persona;
    }

    private Consulta crearConsulta(
            Persona persona,
            EstadoConsulta estado,
            Asesor asesor,
            Estudiante estudiante,
            Monitor monitor,
            String descripcion,
            LocalDate fecha) {
        Consulta consulta = new Consulta();
        consulta.setFecha(fecha);
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
        consulta.setTipo(tipo);
        consulta.setAsesor(asesor);
        consulta.setEstudiante(estudiante);
        consulta.setMonitor(monitor);
        entityManager.persist(consulta);
        return consulta;
    }

    private Proceso crearProceso(
            Consulta consulta,
            Departamento departamento,
            OrganoControl organoControl,
            Especialidad especialidad,
            EstadoProceso estado,
            String numeroRadicado,
            boolean activo,
            LocalDateTime fechaCreacion) {
        Proceso proceso = new Proceso();
        proceso.setConsulta(consulta);
        proceso.setDepartamento(departamento);
        proceso.setOrganoControl(organoControl);
        proceso.setEspecialidad(especialidad);
        proceso.setEstado(estado);
        proceso.setNumeroRadicado(numeroRadicado);
        proceso.setActivo(activo);
        proceso.setFechaCreacion(fechaCreacion);
        entityManager.persist(proceso);
        return proceso;
    }
}
