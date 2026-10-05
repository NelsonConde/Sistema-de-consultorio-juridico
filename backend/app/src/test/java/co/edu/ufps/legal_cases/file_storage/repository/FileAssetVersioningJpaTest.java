package co.edu.ufps.legal_cases.file_storage.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import co.edu.ufps.legal_cases.file_storage.model.FileAsset;
import co.edu.ufps.legal_cases.file_storage.model.FileAssetStatus;
import co.edu.ufps.legal_cases.security.model.access.Rol;
import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;
import jakarta.persistence.EntityManager;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=\"DB_consultorioJuridico\"",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FileAssetVersioningJpaTest {

    @Container
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("legal_cases_file_test")
                    .withUsername("legal_cases")
                    .withPassword("legal_cases");

    @DynamicPropertySource
    static void configurarPostgreSql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "\"DB_consultorioJuridico\"");
        registry.add("spring.jpa.properties.hibernate.hbm2ddl.create_namespaces", () -> "true");
        registry.add("spring.jpa.show-sql", () -> "false");
    }

    @Autowired
    private FileAssetRepository repository;

    @Autowired
    private EntityManager entityManager;

    private UsuarioSistema usuario;

    @BeforeEach
    void setUp() {
        Rol rol = new Rol();
        rol.setNombre("ROL_TEST_FILE");
        rol.setTipoPerfil(TipoPerfilUsuario.ADMINISTRATIVO);
        entityManager.persist(rol);

        usuario = new UsuarioSistema();
        usuario.setUsername("file_tester@example.com");
        usuario.setPasswordHash("hash");
        usuario.setRol(rol);
        entityManager.persist(usuario);
        entityManager.flush();
    }

    @Test
    void debePersistirYConsultarVersionesDocumentales() {
        UUID docLogico = UUID.randomUUID();

        // Versión 1
        FileAsset v1 = new FileAsset();
        v1.setBucket("test-bucket");
        v1.setObjectKey("consulta/1/" + UUID.randomUUID() + "-doc.pdf");
        v1.setUploadId(UUID.randomUUID());
        v1.setResourceType("CONSULTA");
        v1.setResourceId(1L);
        v1.setOriginalFileName("doc.pdf");
        v1.setContentType("application/pdf");
        v1.setSize(1024L);
        v1.setChecksum("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        v1.setUploadedBy(usuario);
        v1.setDocumentoLogico(docLogico);
        v1.setVersion(1);
        v1.setTipoDocumental("CONSULTA_ANEXO");
        v1.setOrigen("CARGA_USUARIO");
        v1.setStatus(FileAssetStatus.HISTORICO);
        v1.setActive(false);
        repository.save(v1);

        // Versión 2 (Vigente)
        FileAsset v2 = new FileAsset();
        v2.setBucket("test-bucket");
        v2.setObjectKey("consulta/1/" + UUID.randomUUID() + "-doc_v2.pdf");
        v2.setUploadId(UUID.randomUUID());
        v2.setResourceType("CONSULTA");
        v2.setResourceId(1L);
        v2.setOriginalFileName("doc_v2.pdf");
        v2.setContentType("application/pdf");
        v2.setSize(2048L);
        v2.setChecksum("a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3");
        v2.setUploadedBy(usuario);
        v2.setDocumentoLogico(docLogico);
        v2.setVersion(2);
        v2.setTipoDocumental("CONSULTA_ANEXO");
        v2.setOrigen("CARGA_USUARIO");
        v2.setReferenciaAnterior(v1);
        v2.setStatus(FileAssetStatus.VIGENTE);
        v2.setActive(true);
        repository.save(v2);

        entityManager.flush();
        entityManager.clear();

        // 1. Consultar vigente
        Optional<FileAsset> optVigente = repository.findByDocumentoLogicoAndStatus(docLogico, FileAssetStatus.VIGENTE);
        assertTrue(optVigente.isPresent());
        assertEquals(2, optVigente.get().getVersion());
        assertNotNull(optVigente.get().getReferenciaAnterior());
        assertEquals(v1.getId(), optVigente.get().getReferenciaAnterior().getId());

        // 2. Máxima versión
        Integer maxVer = repository.findMaxVersionByDocumentoLogico(docLogico);
        assertEquals(2, maxVer);

        // 3. Listar versiones ordenadas desc
        List<FileAsset> versiones = repository.findByDocumentoLogicoOrderByVersionDesc(docLogico);
        assertEquals(2, versiones.size());
        assertEquals(2, versiones.get(0).getVersion());
        assertEquals(1, versiones.get(1).getVersion());
    }
}
