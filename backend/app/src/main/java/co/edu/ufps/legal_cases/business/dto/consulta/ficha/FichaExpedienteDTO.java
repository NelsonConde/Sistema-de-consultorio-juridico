package co.edu.ufps.legal_cases.business.dto.consulta.ficha;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO que consolida la totalidad de la información vigente del expediente (PB-35).
 * Agrupa identificación, estado/resultado, partes, clasificación, responsables,
 * seguimientos, proceso judicial, conciliación e índice documental sin exponer
 * rutas internas ni copias de archivos.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FichaExpedienteDTO {

    private IdentificacionFichaDTO identificacion;
    private EstadoResultadoFichaDTO estadoResultado;
    private PartesFichaDTO partes;
    private ClasificacionFichaDTO clasificacion;
    private ResponsablesFichaDTO responsables;

    @Builder.Default
    private List<SeguimientoFichaDTO> seguimientos = new ArrayList<>();

    @Builder.Default
    private List<ProcesoFichaDTO> procesos = new ArrayList<>();

    @Builder.Default
    private List<ConciliacionFichaDTO> conciliaciones = new ArrayList<>();

    @Builder.Default
    private List<DocumentoIndiceDTO> indiceDocumental = new ArrayList<>();

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdentificacionFichaDTO {
        private Long consultaId;
        private LocalDate fechaRadicacion;
        private String tramite;
        private String descripcion;
        private String hechos;
        private String pretensiones;
        private String conceptoJuridico;
        private String observaciones;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstadoResultadoFichaDTO {
        private String estado;
        private String resultado;
        private LocalDate ultimaActualizacion;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartesFichaDTO {
        private PersonaDetalleDTO solicitantePrincipal;
        @Builder.Default
        private List<PersonaDetalleDTO> partesAdicionales = new ArrayList<>();
        @Builder.Default
        private List<PersonaDetalleDTO> contrapartes = new ArrayList<>();
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonaDetalleDTO {
        private Long id;
        private String nombreCompleto;
        private String tipoDocumento;
        private String numeroDocumento;
        private String telefono;
        private String correo;
        private String direccion;
        private String municipio;
        private String barrio;
        private Integer estrato;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClasificacionFichaDTO {
        private String sede;
        private String area;
        private String tema;
        private String tipo;
        private String tipoViolencia;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponsablesFichaDTO {
        private ResponsableDetalleDTO estudiante;
        private ResponsableDetalleDTO asesor;
        private ResponsableDetalleDTO monitor;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponsableDetalleDTO {
        private Long id;
        private String nombre;
        private String documento;
        private String email;
        private String telefono;
        private String codigo;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeguimientoFichaDTO {
        private Long id;
        private LocalDateTime fechaCreacion;
        private LocalDate fechaEntrega;
        private String categoria;
        private String estado;
        private String autor;
        private String descripcion;
        private Boolean alertaDisciplinaria;
        @Builder.Default
        private List<RespuestaFichaDTO> respuestas = new ArrayList<>();
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RespuestaFichaDTO {
        private Long id;
        private LocalDateTime fechaCreacion;
        private String estado;
        private String contenido;
        private String observacionRevision;
        private String estudianteNombre;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcesoFichaDTO {
        private Long id;
        private String numeroRadicado;
        private String departamento;
        private String organoControl;
        private String especialidad;
        private String estado;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConciliacionFichaDTO {
        private Long id;
        private String estado;
        private LocalDateTime fechaConciliacion;
        private LocalDateTime fechaCreacion;
        private String conciliadorNombre;
        private String estudianteNombre;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentoIndiceDTO {
        private Long id;
        private String nombreArchivo;
        private String recursoOrigen;
        private Long recursoId;
        private String contentType;
        private Long tamañoBytes;
        private String tamañoFormateado;
        private LocalDateTime fechaCarga;
        private String cargadoPor;
    }
}
