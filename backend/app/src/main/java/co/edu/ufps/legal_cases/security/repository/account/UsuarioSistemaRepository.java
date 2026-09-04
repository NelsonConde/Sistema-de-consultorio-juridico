package co.edu.ufps.legal_cases.security.repository.account;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.ufps.legal_cases.security.model.account.TipoPerfilUsuario;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;

@Repository
public interface UsuarioSistemaRepository extends JpaRepository<UsuarioSistema, Long> {

        Optional<UsuarioSistema> findByUsernameIgnoreCase(String username);

        boolean existsByUsernameIgnoreCase(String username);

        // Aqui sobreescribe para poder cargar el rol y los permisos con el usuario
        @Override
        @EntityGraph(attributePaths = { "rol", "rol.permisos" })
        List<UsuarioSistema> findAll();

        // Tambien sobreescribe para cargar el rol y permisos, pero solo los activos
        @EntityGraph(attributePaths = { "rol", "rol.permisos" })
        List<UsuarioSistema> findByActivoTrue();

        @Query(value = """
                        SELECT u.id AS id,
                               u.username AS username,
                               u.activo AS activo,
                               r.id AS rolId,
                               r.nombre AS rolNombre,
                               u.tipoPerfilActual AS tipoPerfil
                        FROM UsuarioSistema u
                        JOIN u.rol r
                        WHERE (
                                CAST(:search AS String) IS NULL
                                OR LOWER(u.username)
                                   LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                                OR LOWER(r.nombre)
                                   LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                          )
                          AND (:activo IS NULL OR u.activo = :activo)
                          AND (:tipoPerfil IS NULL OR u.tipoPerfilActual = :tipoPerfil)
                        """, countQuery = """
                        SELECT COUNT(u.id)
                        FROM UsuarioSistema u
                        JOIN u.rol r
                        WHERE (
                                CAST(:search AS String) IS NULL
                                OR LOWER(u.username)
                                   LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                                OR LOWER(r.nombre)
                                   LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                          )
                          AND (:activo IS NULL OR u.activo = :activo)
                          AND (:tipoPerfil IS NULL OR u.tipoPerfilActual = :tipoPerfil)
                        """)
        Page<UsuarioSistemaResumenProjection> buscarResumenPaginado(
                        @Param("search") String search,
                        @Param("activo") Boolean activo,
                        @Param("tipoPerfil") TipoPerfilUsuario tipoPerfil,
                        Pageable pageable);

        @EntityGraph(attributePaths = { "rol", "rol.permisos" })
        Optional<UsuarioSistema> findWithRolAndPermisosById(Long id);

        // Tambien se puede usar para cargar el rol y permisos al buscar por username
        @EntityGraph(attributePaths = { "rol", "rol.permisos" })
        Optional<UsuarioSistema> findWithRolAndPermisosByUsernameIgnoreCase(String username);

        // Esto es para que no solo el usuario del sistema se cargue sino sus permisos.
        // El nombre del metodo se conserva temporalmente para no romper el filtro JWT.
        // El perfil asociado ahora se resuelve con PerfilUsuarioResolverService usando
        // tipo_perfil_actual y usuario_sistema_id en la tabla real.
        @EntityGraph(attributePaths = {
                        "rol",
                        "rol.permisos"
        })
        // Selecciona al que tenga el username que se pide en el Param
        @Query("""
                        SELECT u
                        FROM UsuarioSistema u
                        WHERE u.username = :username
                        """)
        Optional<UsuarioSistema> findWithRolPermisosAndPerfilByUsername(
                        @Param("username") String username); // El param es para usarlo como variable en la consulta

        // Carga el usuario para recuperación de contraseña con rol,
        // sin cargar los permisos del rol ni el perfil real porque no se necesitan en este proceso.
        @EntityGraph(attributePaths = {
                        "rol"
        })
        Optional<UsuarioSistema> findForPasswordResetByUsernameIgnoreCase(String username);
}
