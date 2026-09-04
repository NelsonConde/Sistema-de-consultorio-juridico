package co.edu.ufps.legal_cases.business.qa;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.ufps.legal_cases.business.dto.consulta.ConsultaBusquedaDTO;
import co.edu.ufps.legal_cases.business.dto.persona.PersonaResumenDTO;
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
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaRepository;
import co.edu.ufps.legal_cases.business.repository.consulta.ConsultaResumenProjection;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaRepository;
import co.edu.ufps.legal_cases.business.repository.persona.PersonaResumenProjection;
import co.edu.ufps.legal_cases.business.service.consulta.consulta.ConsultaMapper;
import co.edu.ufps.legal_cases.business.service.persona.persona.PersonaResumenMapper;
import co.edu.ufps.legal_cases.common.dto.PageResponseDTO;
import co.edu.ufps.legal_cases.support.PostgreSqlIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=\"DB_consultorioJuridico\"",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfSystemProperty(named = "scrum267.performance", matches = "true")
class PaginationPerformanceEvidenceTest extends PostgreSqlIntegrationTest {

    private static final int DATASET_ROWS = 240;
    private static final int ARCHIVED_CONSULTAS = 10;
    private static final int PAGE_SIZE = 10;
    private static final int WARMUPS = 5;
    private static final int ITERATIONS = 30;
    private static final Path REPORT_PATH = Path.of("target", "scrum-267-performance-report.md");

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final ConsultaMapper consultaMapper = new ConsultaMapper();
    private final PersonaResumenMapper personaResumenMapper = new PersonaResumenMapper();

    @Autowired
    private ConsultaRepository consultaRepository;

    @Autowired
    private PersonaRepository personaRepository;

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
    private Asesor asesor;
    private Estudiante estudiante;
    private Monitor monitor;

    @BeforeEach
    void setUp() {
        crearCatalogos();
        crearPerfiles();
        crearDatasetSintetico();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void generarEvidenciaReproducibleDeRendimientoAntesYDespues() throws Exception {
        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.setStatisticsEnabled(true);

        EvidenceRow consultasBefore = medir("Consultas", "BEFORE SCRUM-265", DATASET_ROWS,
                this::consultasBefore, statistics);
        EvidenceRow consultasAfter = medir("Consultas", "AFTER SCRUM-265", DATASET_ROWS,
                this::consultasAfter, statistics);
        EvidenceRow personasBefore = medir("Personas", "BEFORE SCRUM-264", DATASET_ROWS,
                this::personasBefore, statistics);
        EvidenceRow personasAfter = medir("Personas", "AFTER SCRUM-264", DATASET_ROWS,
                this::personasAfter, statistics);

        List<EvidenceRow> rows = List.of(consultasBefore, consultasAfter, personasBefore, personasAfter);

        assertAll(
                () -> assertEquals(DATASET_ROWS, consultasBefore.totalElements()),
                () -> assertEquals(DATASET_ROWS, consultasBefore.responseRows()),
                () -> assertEquals(DATASET_ROWS, consultasAfter.totalElements()),
                () -> assertEquals(PAGE_SIZE, consultasAfter.responseRows()),
                () -> assertEquals(DATASET_ROWS, personasBefore.totalElements()),
                () -> assertEquals(PAGE_SIZE, personasBefore.responseRows()),
                () -> assertEquals(DATASET_ROWS, personasAfter.totalElements()),
                () -> assertEquals(PAGE_SIZE, personasAfter.responseRows()),
                () -> rows.forEach(row -> {
                    assertTrue(row.payloadBytes() > 0);
                    assertTrue(row.sqlStatements() > 0);
                    assertTrue(row.p50Ms() > 0);
                    assertTrue(row.p95Ms() > 0);
                }));

        String report = construirReporte(rows, consultasBefore, consultasAfter, personasBefore, personasAfter);
        System.out.println(report);
        escribirReporte(report);
    }

    private EvidenceRow medir(
            String modulo,
            String variante,
            int datasetRows,
            EvidenceOperation operation,
            Statistics statistics) throws Exception {

        for (int i = 0; i < WARMUPS; i++) {
            entityManager.clear();
            OperationOutput warmup = operation.run();
            assertNotNull(warmup);
        }

        List<Long> nanos = new ArrayList<>(ITERATIONS);
        OperationOutput lastOutput = null;

        for (int i = 0; i < ITERATIONS; i++) {
            entityManager.clear();

            long start = System.nanoTime();
            lastOutput = operation.run();
            long elapsed = System.nanoTime() - start;

            nanos.add(elapsed);
            assertNotNull(lastOutput);
        }

        entityManager.clear();
        statistics.clear();
        OperationOutput sqlOutput = operation.run();
        long sqlStatements = statistics.getPrepareStatementCount();

        return new EvidenceRow(
                modulo,
                variante,
                datasetRows,
                sqlOutput.responseRows(),
                sqlOutput.totalElements(),
                sqlOutput.payloadBytes(),
                sqlStatements,
                millis(percentile(nanos, 50)),
                millis(percentile(nanos, 95)));
    }

    private OperationOutput consultasBefore() throws IOException {
        List<ConsultaBusquedaDTO> response = consultaRepository.buscarParaAdministrador(null).stream()
                .map(consultaMapper::convertirABusquedaDTO)
                .toList();

        return new OperationOutput(
                response.size(),
                response.size(),
                objectMapper.writeValueAsBytes(response).length);
    }

    private OperationOutput consultasAfter() throws IOException {
        Page<ConsultaResumenProjection> page = consultaRepository.buscarResumenPaginado(
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                EstadoConsulta.ARCHIVADO,
                PageRequest.of(0, PAGE_SIZE, sortConsultaAfter()));

        List<ConsultaBusquedaDTO> content = page.getContent().stream()
                .map(consultaMapper::convertirABusquedaDTO)
                .toList();
        PageResponseDTO<ConsultaBusquedaDTO> response = new PageResponseDTO<>(
                content,
                1,
                PAGE_SIZE,
                page.getTotalElements(),
                page.getTotalPages());

        return new OperationOutput(
                response.content().size(),
                response.totalElements(),
                objectMapper.writeValueAsBytes(response).length);
    }

    private OperationOutput personasBefore() throws IOException {
        String search = null;
        Boolean activo = null;

        List<Tuple> tuples = entityManager.createQuery("""
                SELECT p.id AS id,
                       p.nombres AS nombres,
                       p.apellidos AS apellidos,
                       p.tipoDocumento AS tipoDocumento,
                       p.numeroDocumento AS numeroDocumento,
                       tipoPersona.nombre AS tipoPersona,
                       p.activo AS activo
                FROM Persona p
                JOIN p.tipoPersona tipoPersona
                WHERE (:activo IS NULL OR p.activo = :activo)
                  AND (
                        CAST(:search AS String) IS NULL
                        OR LOWER(p.nombres) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                        OR LOWER(p.apellidos) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                        OR LOWER(CONCAT(CONCAT(p.nombres, ' '), p.apellidos))
                            LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                        OR LOWER(p.numeroDocumento) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                  )
                ORDER BY LOWER(p.nombres), LOWER(p.apellidos), p.id
                """, Tuple.class)
                .setParameter("search", search)
                .setParameter("activo", activo)
                .setFirstResult(0)
                .setMaxResults(PAGE_SIZE)
                .getResultList();

        Long total = entityManager.createQuery("""
                SELECT COUNT(p.id)
                FROM Persona p
                WHERE (:activo IS NULL OR p.activo = :activo)
                  AND (
                        CAST(:search AS String) IS NULL
                        OR LOWER(p.nombres) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                        OR LOWER(p.apellidos) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                        OR LOWER(CONCAT(CONCAT(p.nombres, ' '), p.apellidos))
                            LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                        OR LOWER(p.numeroDocumento) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                  )
                """, Long.class)
                .setParameter("search", search)
                .setParameter("activo", activo)
                .getSingleResult();

        List<PersonaResumenDTO> content = tuples.stream()
                .map(this::personaResumenDesdeTuple)
                .toList();
        PageResponseDTO<PersonaResumenDTO> response = new PageResponseDTO<>(
                content,
                1,
                PAGE_SIZE,
                total,
                totalPages(total));

        return new OperationOutput(
                response.content().size(),
                response.totalElements(),
                objectMapper.writeValueAsBytes(response).length);
    }

    private OperationOutput personasAfter() throws IOException {
        Page<PersonaResumenProjection> page = personaRepository.buscarResumen(
                null,
                null,
                true,
                null,
                null,
                EstadoConsulta.ARCHIVADO,
                PageRequest.of(0, PAGE_SIZE, sortPersonaAfter()));

        List<PersonaResumenDTO> content = page.getContent().stream()
                .map(personaResumenMapper::convertirAResumen)
                .toList();
        PageResponseDTO<PersonaResumenDTO> response = new PageResponseDTO<>(
                content,
                1,
                PAGE_SIZE,
                page.getTotalElements(),
                page.getTotalPages());

        return new OperationOutput(
                response.content().size(),
                response.totalElements(),
                objectMapper.writeValueAsBytes(response).length);
    }

    private PersonaResumenDTO personaResumenDesdeTuple(Tuple tuple) {
        return new PersonaResumenDTO(
                tuple.get("id", Long.class),
                tuple.get("nombres", String.class),
                tuple.get("apellidos", String.class),
                tuple.get("tipoDocumento", String.class),
                enmascararDocumento(tuple.get("numeroDocumento", String.class)),
                tuple.get("tipoPersona", String.class),
                tuple.get("activo", Boolean.class));
    }

    private String enmascararDocumento(String numeroDocumento) {
        if (numeroDocumento == null || numeroDocumento.isBlank()) {
            return null;
        }

        if (numeroDocumento.length() <= 4) {
            return "*".repeat(numeroDocumento.length());
        }

        int inicioVisible = numeroDocumento.length() - 4;
        return "*".repeat(inicioVisible) + numeroDocumento.substring(inicioVisible);
    }

    private void escribirReporte(String report) throws IOException {
        Files.createDirectories(REPORT_PATH.getParent());
        Files.writeString(REPORT_PATH, report, StandardCharsets.UTF_8);
    }

    private String construirReporte(
            List<EvidenceRow> rows,
            EvidenceRow consultasBefore,
            EvidenceRow consultasAfter,
            EvidenceRow personasBefore,
            EvidenceRow personasAfter) {

        StringBuilder report = new StringBuilder();
        report.append("# SCRUM-267 \u2014 Evidencia de rendimiento\n\n");
        report.append("Entorno:\n");
        report.append("- PostgreSQL Testcontainers\n");
        report.append("- postgres:16-alpine\n");
        report.append("- Java 21\n");
        report.append("- dataset sint\u00e9tico\n");
        report.append("- warmups: ").append(WARMUPS).append('\n');
        report.append("- iteraciones: ").append(ITERATIONS).append("\n\n");
        report.append("| M\u00f3dulo | Variante | Dataset | Filas respuesta | Payload bytes | SQL | p50 ms | p95 ms |\n");
        report.append("| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |\n");
        rows.forEach(row -> report.append(row.toMarkdown()).append('\n'));
        report.append("\n## Interpretaci\u00f3n\n\n");
        report.append("Los valores anteriores corresponden a esta ejecuci\u00f3n local y deben ")
                .append("interpretarse comparativamente dentro de las mismas condiciones.\n\n");
        report.append("- Consultas AFTER vs BEFORE: ")
                .append(delta("bytes", consultasBefore.payloadBytes(), consultasAfter.payloadBytes())).append(", ")
                .append(delta("p50", consultasBefore.p50Ms(), consultasAfter.p50Ms())).append(", ")
                .append(delta("p95", consultasBefore.p95Ms(), consultasAfter.p95Ms())).append(", ")
                .append(delta("SQL", consultasBefore.sqlStatements(), consultasAfter.sqlStatements())).append(".\n");
        report.append("- Personas AFTER vs BEFORE: ")
                .append(delta("bytes", personasBefore.payloadBytes(), personasAfter.payloadBytes())).append(", ")
                .append(delta("p50", personasBefore.p50Ms(), personasAfter.p50Ms())).append(", ")
                .append(delta("p95", personasBefore.p95Ms(), personasAfter.p95Ms())).append(", ")
                .append(delta("SQL", personasBefore.sqlStatements(), personasAfter.sqlStatements())).append(".\n");
        report.append("\nresponse_rows representa el n\u00famero de elementos incluidos en la respuesta JSON, ")
                .append("no filas le\u00eddas f\u00edsicamente por PostgreSQL.\n");
        report.append("p50 y p95 se calculan ordenando las mediciones de la misma ejecuci\u00f3n y tomando ")
                .append("el \u00edndice ceil(percentil * n) - 1.\n");

        return report.toString();
    }

    private String delta(String label, double before, double after) {
        if (before == 0) {
            return label + " delta_pct no calculado";
        }

        double percentage = ((after - before) / before) * 100.0;
        return label + " delta_pct " + format(percentage) + "%";
    }

    private long percentile(List<Long> nanos, int percentile) {
        List<Long> sorted = nanos.stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        int index = (int) Math.ceil((percentile / 100.0) * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    private double millis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private int totalPages(long totalElements) {
        return (int) Math.ceil(totalElements / (double) PAGE_SIZE);
    }

    private Sort sortConsultaAfter() {
        return Sort.by(
                Sort.Order.desc("fecha"),
                Sort.Order.asc("id"));
    }

    private Sort sortPersonaAfter() {
        return Sort.by(
                Sort.Order.asc("nombres").ignoreCase(),
                Sort.Order.asc("apellidos").ignoreCase(),
                Sort.Order.asc("id"));
    }

    private void crearDatasetSintetico() {
        List<Persona> personas = new ArrayList<>(DATASET_ROWS);

        for (int i = 1; i <= DATASET_ROWS; i++) {
            Persona persona = crearPersona(i);
            personas.add(persona);
            crearConsulta(persona, estadoNoArchivado(i), i);
        }

        for (int i = 0; i < ARCHIVED_CONSULTAS; i++) {
            crearConsulta(personas.get(i), EstadoConsulta.ARCHIVADO, DATASET_ROWS + i + 1);
        }
    }

    private EstadoConsulta estadoNoArchivado(int index) {
        EstadoConsulta[] estados = {
                EstadoConsulta.ACTIVO,
                EstadoConsulta.PENDIENTE,
                EstadoConsulta.EN_PROCESO,
                EstadoConsulta.CERRADO,
                EstadoConsulta.URGENTE
        };
        return estados[index % estados.length];
    }

    private void crearCatalogos() {
        Departamento departamento = new Departamento();
        departamento.setNombre("Departamento QA");
        entityManager.persist(departamento);

        municipio = new Municipio();
        municipio.setNombre("Municipio QA");
        municipio.setDepartamento(departamento);
        entityManager.persist(municipio);

        barrio = new Barrio();
        barrio.setNombre("Barrio QA");
        barrio.setMunicipio(municipio);
        entityManager.persist(barrio);

        nacionalidad = new Nacionalidad();
        nacionalidad.setNombre("Nacionalidad QA");
        entityManager.persist(nacionalidad);

        tipoPersona = new TipoPersona();
        tipoPersona.setNombre("Solicitante QA");
        entityManager.persist(tipoPersona);

        condicion = new Condicion();
        condicion.setNombre("Condicion QA");
        entityManager.persist(condicion);

        ocupacion = new Ocupacion();
        ocupacion.setNombre("Ocupacion QA");
        entityManager.persist(ocupacion);

        empresa = new Empresa();
        empresa.setNombre("Empresa QA");
        entityManager.persist(empresa);

        sede = new Sede();
        sede.setNombre("Sede QA");
        entityManager.persist(sede);

        area = new Area();
        area.setNombre("Area QA");
        entityManager.persist(area);

        tema = new Tema();
        tema.setNombre("Tema QA");
        tema.setArea(area);
        entityManager.persist(tema);

        tipo = new Tipo();
        tipo.setNombre("Tipo QA");
        tipo.setTema(tema);
        entityManager.persist(tipo);

        tipoDocumentoPerfil = new TipoDocumento();
        tipoDocumentoPerfil.setNombre("Cedula QA");
        entityManager.persist(tipoDocumentoPerfil);
    }

    private void crearPerfiles() {
        asesor = new Asesor();
        asesor.setNombre("Asesor QA");
        asesor.setTipoDocumento(tipoDocumentoPerfil);
        asesor.setDocumento("ASE-QA-001");
        asesor.setEmail("asesor.qa@example.test");
        asesor.setTelefono("3000000001");
        asesor.setUsuario("asesor.qa");
        asesor.setSede(sede);
        asesor.setCodigo("ASE-QA");
        asesor.setArea(area);
        entityManager.persist(asesor);

        estudiante = new Estudiante();
        estudiante.setNombre("Estudiante QA");
        estudiante.setTipoDocumento(tipoDocumentoPerfil);
        estudiante.setDocumento("EST-QA-001");
        estudiante.setEmail("estudiante.qa@example.test");
        estudiante.setTelefono("3100000001");
        estudiante.setUsuario("estudiante.qa");
        estudiante.setSede(sede);
        estudiante.setCodigo("EST-QA");
        estudiante.setAsesor(asesor);
        entityManager.persist(estudiante);

        monitor = new Monitor();
        monitor.setNombre("Monitor QA");
        monitor.setTipoDocumento(tipoDocumentoPerfil);
        monitor.setDocumento("MON-QA-001");
        monitor.setEmail("monitor.qa@example.test");
        monitor.setTelefono("3200000001");
        monitor.setUsuario("monitor.qa");
        monitor.setCodigo("MON-QA");
        monitor.setSede(sede);
        entityManager.persist(monitor);
    }

    private Persona crearPersona(int index) {
        String suffix = "%04d".formatted(index);

        Persona persona = new Persona();
        persona.setTipoPersona(tipoPersona);
        persona.setTipoDocumento("CC");
        persona.setNumeroDocumento("DOC-" + suffix);
        persona.setFechaExpedicion(LocalDate.of(2010, 1, 1));
        persona.setCiudadExpedicion("Municipio QA");
        persona.setNombres("Persona" + suffix);
        persona.setApellidos("Apellido" + suffix);
        persona.setNombreIdentitario("Persona" + suffix);
        persona.setPronombre("no informa");
        persona.setSexo("no informa");
        persona.setGenero("no informa");
        persona.setOrientacionSexual("no informa");
        persona.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        persona.setTelefono("300" + suffix);
        persona.setCorreo("persona" + suffix + "@example.test");
        persona.setNacionalidad(nacionalidad);
        persona.setEstadoCivil("no informa");
        persona.setEscolaridad("no informa");
        persona.setGrupoEtnico("no informa");
        persona.setCondicionActual(condicion);
        persona.setSabeLeerEscribir(true);
        persona.setDiscapacidad("ninguna");
        persona.setCaracterizacionPcd("no aplica");
        persona.setNecesitaAjustePcd(false);
        persona.setMunicipio(municipio);
        persona.setBarrio(barrio);
        persona.setDireccion("Direccion QA " + suffix);
        persona.setComuna("QA");
        persona.setLocalidad("QA");
        persona.setEstrato(2);
        persona.setTipoVivienda("no informa");
        persona.setZona("urbana");
        persona.setTenencia("no informa");
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
        persona.setComoSeEntero("QA");
        persona.setRelacionConUniversidad("Ninguna");
        persona.setActivo(true);
        entityManager.persist(persona);
        return persona;
    }

    private Consulta crearConsulta(Persona persona, EstadoConsulta estado, int index) {
        String suffix = "%04d".formatted(index);

        Consulta consulta = new Consulta();
        consulta.setFecha(LocalDate.of(2026, 1, 1).plusDays(index % 30));
        consulta.setDescripcion("Consulta sintetica " + suffix);
        consulta.setHechos("Hechos sinteticos " + suffix);
        consulta.setPretensiones("Pretensiones sinteticas " + suffix);
        consulta.setConceptoJuridico("Concepto sintetico " + suffix);
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

    @FunctionalInterface
    private interface EvidenceOperation {

        OperationOutput run() throws Exception;
    }

    private record OperationOutput(
            int responseRows,
            long totalElements,
            int payloadBytes) {
    }

    private record EvidenceRow(
            String modulo,
            String variante,
            int datasetRows,
            int responseRows,
            long totalElements,
            int payloadBytes,
            long sqlStatements,
            double p50Ms,
            double p95Ms) {

        private static final DecimalFormat DECIMAL_FORMAT =
                new DecimalFormat("0.###", DecimalFormatSymbols.getInstance(Locale.US));

        String toMarkdown() {
            return "| %s | %s | %d | %d | %d | %d | %s | %s |".formatted(
                    modulo,
                    variante,
                    datasetRows,
                    responseRows,
                    payloadBytes,
                    sqlStatements,
                    format(p50Ms),
                    format(p95Ms));
        }
    }

    private static String format(double value) {
        return EvidenceRow.DECIMAL_FORMAT.format(value);
    }
}
