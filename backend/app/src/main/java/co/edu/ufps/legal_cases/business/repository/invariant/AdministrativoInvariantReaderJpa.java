package co.edu.ufps.legal_cases.business.repository.invariant;

import java.util.List;

import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.service.invariant.administracion.port.AdministrativoInvariantReader;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;

/**
 * Obtiene desde JPA el estado administrativo requerido por las invariantes,
 * exponiendo únicamente los datos necesarios para su evaluación.
 */
@Repository
public class AdministrativoInvariantReaderJpa
        implements AdministrativoInvariantReader {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<AdministrativoLectura> cargarEstadoAdministrativo() {

        return entityManager.createQuery("""
                        SELECT
                            a.id,
                            u.id,
                            u.activo,
                            u.tipoPerfilActual,
                            a.activo,
                            a.directora,
                            r.id
                        FROM Administrativo a
                        LEFT JOIN a.usuarioSistema u
                        LEFT JOIN u.rol r
                        ORDER BY a.id
                        """, Tuple.class)
                .getResultList()
                .stream()
                .map(this::convertir)
                .toList();
    }

    private AdministrativoLectura convertir(Tuple fila) {

        return new AdministrativoLectura(
                fila.get(0, Long.class),
                fila.get(1, Long.class),
                fila.get(2, Boolean.class),
                fila.get(3, TipoPerfilUsuario.class),
                fila.get(4, Boolean.class),
                fila.get(5, Boolean.class),
                fila.get(6, Long.class));
    }
}