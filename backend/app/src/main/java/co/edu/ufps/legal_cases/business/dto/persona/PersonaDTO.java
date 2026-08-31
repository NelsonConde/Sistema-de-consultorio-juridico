package co.edu.ufps.legal_cases.business.dto.persona;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PersonaDTO {

    private Long id;

    // Informacion basica
    @NotNull(message = "El tipo de persona es obligatorio")
    private Long tipoPersonaId;

    @NotBlank(message = "El tipo de documento es obligatorio")
    @Size(max = 10, message = "El tipo de documento no puede superar los 10 caracteres")
    private String tipoDocumento;

    @NotBlank(message = "El numero de documento es obligatorio")
    @Size(max = 30, message = "El numero de documento no puede superar los 30 caracteres")
    private String numeroDocumento;

    @NotNull(message = "La fecha de expedicion es obligatoria")
    private LocalDate fechaExpedicion;

    @NotBlank(message = "La ciudad de expedicion es obligatoria")
    @Size(max = 100, message = "La ciudad de expedicion no puede superar los 100 caracteres")
    private String ciudadExpedicion;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100, message = "Los nombres no pueden superar los 100 caracteres")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres")
    private String apellidos;

    @NotBlank(message = "El nombre identitario es obligatorio")
    @Size(max = 100, message = "El nombre identitario no puede superar los 100 caracteres")
    private String nombreIdentitario;

    @NotBlank(message = "El pronombre es obligatorio")
    @Size(max = 50, message = "El pronombre no puede superar los 50 caracteres")
    private String pronombre;

    @NotBlank(message = "El sexo es obligatorio")
    @Size(max = 20, message = "El sexo no puede superar los 20 caracteres")
    private String sexo;

    @NotBlank(message = "El genero es obligatorio")
    @Size(max = 20, message = "El genero no puede superar los 20 caracteres")
    private String genero;

    @NotBlank(message = "La orientacion sexual es obligatoria")
    @Size(max = 50, message = "La orientacion sexual no puede superar los 50 caracteres")
    private String orientacionSexual;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    private LocalDate fechaNacimiento;

    @Size(max = 30, message = "El telefono no puede superar los 30 caracteres")
    private String telefono;

    @Email(message = "El correo no tiene un formato valido")
    @Size(max = 120, message = "El correo no puede superar los 120 caracteres")
    private String correo;

    @NotNull(message = "La nacionalidad es obligatoria")
    private Long nacionalidadId;

    @NotBlank(message = "El estado civil es obligatorio")
    @Size(max = 30, message = "El estado civil no puede superar los 30 caracteres")
    private String estadoCivil;

    @NotBlank(message = "La escolaridad es obligatoria")
    @Size(max = 100, message = "La escolaridad no puede superar los 100 caracteres")
    private String escolaridad;

    @NotBlank(message = "El grupo etnico es obligatorio")
    @Size(max = 100, message = "El grupo etnico no puede superar los 100 caracteres")
    private String grupoEtnico;

    @NotNull(message = "La condicion actual es obligatoria")
    private Long condicionActualId;

    @NotNull(message = "Debe indicar si sabe leer y escribir")
    private Boolean sabeLeerEscribir;

    @NotBlank(message = "La discapacidad es obligatoria")
    @Size(max = 100, message = "La discapacidad no puede superar los 100 caracteres")
    private String discapacidad;

    @NotBlank(message = "La caracterizacion PCD es obligatoria")
    @Size(max = 150, message = "La caracterizacion PCD no puede superar los 150 caracteres")
    private String caracterizacionPcd;

    @NotNull(message = "Debe indicar si necesita ajuste PCD")
    private Boolean necesitaAjustePcd;

    // Informacion de vivienda
    // El departamento se resuelve desde el municipio; el frontend lo usa para filtrar el combo de municipios.
    private Long departamentoId;

    @NotNull(message = "El municipio es obligatorio")
    private Long municipioId;

    @NotNull(message = "El barrio es obligatorio")
    private Long barrioId;

    @NotBlank(message = "La direccion es obligatoria")
    @Size(max = 150, message = "La direccion no puede superar los 150 caracteres")
    private String direccion;

    @NotBlank(message = "La comuna es obligatoria")
    @Size(max = 100, message = "La comuna no puede superar los 100 caracteres")
    private String comuna;

    @NotBlank(message = "La localidad es obligatoria")
    @Size(max = 100, message = "La localidad no puede superar los 100 caracteres")
    private String localidad;

    @NotNull(message = "El estrato es obligatorio")
    @Min(value = 0, message = "El estrato no puede ser negativo")
    private Integer estrato;

    @NotBlank(message = "El tipo de vivienda es obligatorio")
    @Size(max = 100, message = "El tipo de vivienda no puede superar los 100 caracteres")
    private String tipoVivienda;

    @NotBlank(message = "La zona es obligatoria")
    @Size(max = 50, message = "La zona no puede superar los 50 caracteres")
    private String zona;

    @NotBlank(message = "La tenencia es obligatoria")
    @Size(max = 100, message = "La tenencia no puede superar los 100 caracteres")
    private String tenencia;

    @NotNull(message = "El numero de personas a cargo es obligatorio")
    @Min(value = 0, message = "El numero de personas a cargo no puede ser negativo")
    private Integer numeroPersonasACargo;

    @NotNull(message = "Debe indicar si tiene ingresos adicionales")
    private Boolean ingresosAdicionales;

    @NotNull(message = "Debe indicar si cuenta con energia electrica")
    private Boolean energiaElectrica;

    @NotNull(message = "Debe indicar si cuenta con acueducto")
    private Boolean acueducto;

    @NotNull(message = "Debe indicar si cuenta con alcantarillado")
    private Boolean alcantarillado;

    // Aspectos economicos
    @NotNull(message = "La ocupacion es obligatoria")
    private Long ocupacionId;

    @NotNull(message = "La empresa es obligatoria")
    private Long empresaId;

    @NotNull(message = "El salario es obligatorio")
    @Min(value = 0, message = "El salario no puede ser negativo")
    private Integer salario;

    @NotBlank(message = "El cargo es obligatorio")
    @Size(max = 100, message = "El cargo no puede superar los 100 caracteres")
    private String cargo;

    @NotBlank(message = "La direccion de la empresa es obligatoria")
    @Size(max = 150, message = "La direccion de la empresa no puede superar los 150 caracteres")
    private String direccionEmpresa;

    @NotBlank(message = "El telefono de la empresa es obligatorio")
    @Size(max = 30, message = "El telefono de la empresa no puede superar los 30 caracteres")
    private String telefonoEmpresa;

    // Datos del acudiente
    @Size(max = 150, message = "El nombre del acudiente no puede superar los 150 caracteres")
    private String nombreCompletoAcudiente;

    @Size(max = 100, message = "La relacion del acudiente no puede superar los 100 caracteres")
    private String relacionAcudiente;

    @Size(max = 30, message = "El telefono del acudiente no puede superar los 30 caracteres")
    private String telefonoAcudiente;

    @Email(message = "El correo del acudiente no tiene un formato valido")
    @Size(max = 120, message = "El correo del acudiente no puede superar los 120 caracteres")
    private String correoAcudiente;

    @Size(max = 150, message = "La direccion del acudiente no puede superar los 150 caracteres")
    private String direccionAcudiente;

    // Informacion del servicio
    @NotBlank(message = "Debe indicar como se entero del servicio")
    @Size(max = 150, message = "El campo como se entero no puede superar los 150 caracteres")
    private String comoSeEntero;

    @NotBlank(message = "La relacion con la universidad es obligatoria")
    @Size(max = 150, message = "La relacion con la universidad no puede superar los 150 caracteres")
    private String relacionConUniversidad;

    private Boolean activo;
}