package co.edu.ufps.legal_cases.security.constant;

// Nombres centralizados de permisos del sistema.
// Evita repetir textos en controllers, services y validaciones de acceso.
public final class PermisoNombre {

    private PermisoNombre() {
    }

    // Permisos antiguos que se conservan temporalmente por compatibilidad.
    public static final String GESTIONAR_USUARIOS = "Gestionar usuarios";
    public static final String GESTIONAR_ROLES = "Gestionar roles";
    public static final String GESTIONAR_PERMISOS = "Gestionar permisos";
    public static final String GESTIONAR_CATALOGOS = "Gestionar catálogos";
    public static final String GESTIONAR_PERSONAS = "Gestionar personas";
    public static final String GESTIONAR_CONSULTAS = "Gestionar consultas";
    public static final String VER_REPORTES = "Ver reportes";

    // Navegación.
    public static final String ACCEDER_INICIO = "Acceder inicio";
    public static final String ACCEDER_RECEPCION = "Acceder recepción";
    public static final String ACCEDER_TAREAS = "Acceder tareas";
    public static final String ACCEDER_NUEVA_CONSULTA = "Acceder nueva consulta";
    public static final String ACCEDER_CONSULTAS_JURIDICAS = "Acceder consultas jurídicas";
    public static final String ACCEDER_ADMINISTRACION = "Acceder administración";
    public static final String ACCEDER_ROLES = "Acceder roles";
    public static final String ACCEDER_ESTUDIANTES = "Acceder estudiantes";
    public static final String ACCEDER_ASESORES_MONITORES = "Acceder asesores y monitores";
    public static final String ACCEDER_PERSONAS = "Acceder personas";
    public static final String ACCEDER_ELIMINACION = "Acceder eliminación";
    public static final String ACCEDER_CONCILIACIONES = "Acceder conciliaciones";
    public static final String ACCEDER_PROCESOS = "Acceder procesos";

    // Catálogos y datos auxiliares.
    public static final String VER_CATALOGOS = "Ver catálogos";
    public static final String VER_PERSONAS = "Ver personas";
    public static final String CREAR_PERSONAS = "Crear personas";
    public static final String EDITAR_PERSONAS = "Editar personas";
    public static final String CAMBIAR_ESTADO_PERSONAS = "Cambiar estado personas";
    public static final String VER_PERFILES_AUXILIARES = "Ver perfiles auxiliares";

    // Consultas.
    public static final String VER_CONSULTAS = "Ver consultas";
    public static final String CREAR_CONSULTAS = "Crear consultas";
    public static final String EDITAR_CONSULTAS = "Editar consultas";
    public static final String CAMBIAR_ESTADO_CONSULTAS = "Cambiar estado consultas";
    public static final String ARCHIVAR_CONSULTAS = "Archivar consultas";
    public static final String ASIGNAR_RESPONSABLES_CONSULTA = "Asignar responsables consulta";

    // Seguimientos / tareas.
    // Tareas es una interfaz de seguimientos; por eso las acciones reales quedan
    // aquí.
    public static final String VER_SEGUIMIENTOS = "Ver seguimientos";
    public static final String CREAR_SEGUIMIENTOS = "Crear seguimientos";
    public static final String EDITAR_SEGUIMIENTOS = "Editar seguimientos";
    public static final String ELIMINAR_SEGUIMIENTOS = "Eliminar seguimientos";
    public static final String RESPONDER_SEGUIMIENTOS = "Responder seguimientos";
    public static final String APROBAR_RESPUESTAS_SEGUIMIENTO = "Aprobar respuestas de seguimiento";
    public static final String VER_ALERTAS_DISCIPLINARIAS = "Ver alertas disciplinarias";
    public static final String GESTIONAR_CATEGORIAS_SEGUIMIENTO = "Gestionar categorías de seguimiento";

    // Usuarios.
    public static final String VER_USUARIOS = "Ver usuarios";
    public static final String CREAR_USUARIOS = "Crear usuarios";
    public static final String EDITAR_USUARIOS = "Editar usuarios";
    public static final String CAMBIAR_ESTADO_USUARIOS = "Cambiar estado usuarios";
    public static final String ASIGNAR_ROL_USUARIOS = "Asignar rol usuarios";

    // Roles y permisos.
    public static final String VER_ROLES = "Ver roles";
    public static final String CREAR_ROLES = "Crear roles";
    public static final String EDITAR_ROLES = "Editar roles";
    public static final String ASIGNAR_PERMISOS_A_ROLES = "Asignar permisos a roles";

    // Estudiantes.
    public static final String VER_ESTUDIANTES = "Ver estudiantes";
    public static final String CAMBIAR_ESTADO_ESTUDIANTES = "Cambiar estado estudiantes";

    // Asesores y monitores.
    public static final String VER_ASESORES_MONITORES = "Ver asesores y monitores";
    public static final String GESTIONAR_ASESORES_MONITORES = "Gestionar asesores y monitores";

    // Conciliaciones.
    public static final String VER_CONCILIACIONES = "Ver conciliaciones";
    public static final String GESTIONAR_CONCILIACIONES = "Gestionar conciliaciones";
    public static final String PROGRAMAR_REUNIONES_CONCILIACION = "Programar reuniones de conciliación";
    public static final String REPROGRAMAR_REUNIONES_CONCILIACION = "Reprogramar reuniones de conciliación";
    public static final String CONCLUIR_CONCILIACIONES = "Concluir conciliaciones";

    // Procesos.
    public static final String VER_PROCESOS = "Ver procesos";
    public static final String GESTIONAR_PROCESOS = "Gestionar procesos";

    // Conciliadores.
    public static final String VER_CONCILIADORES = "Ver conciliadores";
    public static final String GESTIONAR_CONCILIADORES = "Gestionar conciliadores";

    // Administradores.
    // Gestionar administradores tendrá regla adicional: solo directora.
    public static final String VER_ADMINISTRADORES = "Ver administradores";
    public static final String GESTIONAR_ADMINISTRADORES = "Gestionar administradores";
}