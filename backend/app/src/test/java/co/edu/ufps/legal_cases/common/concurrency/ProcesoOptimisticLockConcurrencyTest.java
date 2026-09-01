package co.edu.ufps.legal_cases.common.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.proceso.Proceso;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.OptimisticLockException;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=\"DB_consultorioJuridico\"",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProcesoOptimisticLockConcurrencyTest
        extends Db03PostgreSqlTestBase {

    private static final Long PROCESO_ID = 900001L;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void prepararProceso() throws SQLException {
        /*
         * Solo necesitamos una fila de Proceso.
         *
         * Las relaciones del proceso son LAZY. Para esta prueba aislada
         * insertamos ids técnicos y deshabilitamos temporalmente las FK
         * en esta conexión de prueba.
         *
         * Esto jamás se ejecuta contra la BD real: corre únicamente
         * dentro del PostgreSQL efímero de Testcontainers.
         */
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                statement.execute(
                        "SET session_replication_role = replica");

                statement.executeUpdate("""
                        DELETE FROM "DB_consultorioJuridico".proceso
                        WHERE id = 900001
                        """);

                statement.executeUpdate("""
                        INSERT INTO "DB_consultorioJuridico".proceso (
                            id,
                            version,
                            numero_radicado,
                            departamento_id,
                            consulta_id,
                            organo_control_id,
                            especialidad_id,
                            estado,
                            activo
                        )
                        VALUES (
                            900001,
                            0,
                            'DB03-BASE',
                            900001,
                            900001,
                            NULL,
                            NULL,
                            'PENDIENTE',
                            TRUE
                        )
                        """);

                statement.execute(
                        "SET session_replication_role = origin");

                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    @Test
    void impideQueUnaTransaccionObsoletaSobrescribaLaPrimera() {
        EntityManager emUsuarioA =
                entityManagerFactory.createEntityManager();

        EntityManager emUsuarioB =
                entityManagerFactory.createEntityManager();

        EntityTransaction txA = emUsuarioA.getTransaction();
        EntityTransaction txB = emUsuarioB.getTransaction();

        try {
            /*
             * Ambos usuarios comienzan desde la misma versión.
             */
            txA.begin();
            txB.begin();

            Proceso procesoA =
                    emUsuarioA.find(Proceso.class, PROCESO_ID);

            Proceso procesoB =
                    emUsuarioB.find(Proceso.class, PROCESO_ID);

            assertNotNull(procesoA);
            assertNotNull(procesoB);

            assertEquals(0L, procesoA.getVersion());
            assertEquals(0L, procesoB.getVersion());

            /*
             * Usuario A guarda primero.
             */
            procesoA.setNumeroRadicado("DB03-CAMBIO-A");

            emUsuarioA.flush();
            txA.commit();

            assertEquals(1L, procesoA.getVersion());

            /*
             * Usuario B sigue teniendo la entidad que leyó
             * cuando estaba en versión 0.
             */
            procesoB.setNumeroRadicado("DB03-CAMBIO-B");

            /*
             * Hibernate debe ejecutar un UPDATE equivalente a:
             *
             * UPDATE proceso
             * SET ..., version = 1
             * WHERE id = ? AND version = 0
             *
             * Pero la BD ya está en version = 1.
             *
             * Por eso no debe sobrescribir.
             */
            assertThrows(
                    OptimisticLockException.class,
                    emUsuarioB::flush);

            if (txB.isActive()) {
                txB.rollback();
            }

        } finally {
            if (txA.isActive()) {
                txA.rollback();
            }

            if (txB.isActive()) {
                txB.rollback();
            }

            emUsuarioA.close();
            emUsuarioB.close();
        }

        /*
         * Una tercera transacción verifica qué quedó realmente
         * persistido.
         */
        EntityManager verificacion =
                entityManagerFactory.createEntityManager();

        EntityTransaction txVerificacion =
                verificacion.getTransaction();

        try {
            txVerificacion.begin();

            Proceso persistido =
                    verificacion.find(Proceso.class, PROCESO_ID);

            assertNotNull(persistido);

            assertEquals(
                    "DB03-CAMBIO-A",
                    persistido.getNumeroRadicado());

            assertEquals(
                    1L,
                    persistido.getVersion());

            txVerificacion.commit();

        } finally {
            if (txVerificacion.isActive()) {
                txVerificacion.rollback();
            }

            verificacion.close();
        }
    }
}
