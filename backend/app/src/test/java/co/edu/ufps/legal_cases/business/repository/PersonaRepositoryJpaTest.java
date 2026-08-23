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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

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
import co.edu.ufps.legal_cases.business.model.perfil.Asesor;
import co.edu.ufps.legal_cases.business.model.perfil.Estudiante;
import co.edu.ufps.legal_cases.business.model.perfil.Monitor;
import co.edu.ufps.legal_cases.business.model.persona.Condicion;
import co.edu.ufps.legal_cases.business.model.persona.Empresa;
import co.edu.ufps.legal_cases.business.model.persona.Ocupacion;
import co.edu.ufps.legal_cases.business.model.persona.Persona;
import co.edu.ufps.legal_cases.business.model.persona.TipoPersona;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaConsultaScopeRepository;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaRepository;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaResumenProjection;
import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sec07;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=PUBLIC",
        "spring.jpa.show-sql=false"
})
class PersonaRepositoryJpaTest {

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

    @Test
    void debeBuscarPorNombreApellidoNombreCompletoYDocumento() {
        assertIds(
                personaRepository.buscarResumen(
                        "ana",
                        null,
                        PageRequest.of(0, 10)),
                principal.getId());

        assertIds(
                personaRepository.buscarResumen(
                        "Bermudez",
                        null,
                        PageRequest.of(0, 10)),
                parte.getId());

        assertIds(
                personaRepository.buscarResumen(
                        "Carla Contreras",
                        null,
                        PageRequest.of(0, 10)),
                contraparte.getId());

        assertIds(
                personaRepository.buscarResumen(
                        "1090123456",
                        null,
                        PageRequest.of(0, 10)),
                principal.getId());
    }

    @Test
    void debePaginarContarYOrdenarDeterministicamenteEnBaseDeDatos() {
        Page<PersonaResumenProjection> primeraPagina =
                personaRepository.buscarResumen(
                        null,
                        null,
                        PageRequest.of(0, 2));

        Page<PersonaResumenProjection> segundaPagina =
                personaRepository.buscarResumen(
                        null,
                        null,
                        PageRequest.of(1, 2));

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
    void projectionDebeContenerSoloColumnasDelResumen() {
        Page<PersonaResumenProjection> resultado =
                personaRepository.buscarResumen(
                        "1090123456",
                        null,
                        PageRequest.of(0, 10));

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
                personaRepository.buscarResumen(
                        null,
                        true,
                        PageRequest.of(0, 10));

        assertEquals(5, resultado.getTotalElements());

        assertFalse(
                resultado.getContent()
                        .stream()
                        .anyMatch(item ->
                                item.getId().equals(inactiva.getId())));
    }

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