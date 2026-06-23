package co.edu.ufps.legal_cases.security.service.account.cambio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.ufps.legal_cases.business.model.catalogo.Area;
import co.edu.ufps.legal_cases.business.model.catalogo.Sede;
import co.edu.ufps.legal_cases.business.model.catalogo.TipoDocumento;
import co.edu.ufps.legal_cases.business.repository.catalogo.AreaRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.SedeRepository;
import co.edu.ufps.legal_cases.business.repository.catalogo.TipoDocumentoRepository;
import co.edu.ufps.legal_cases.common.exception.BusinessException;
import co.edu.ufps.legal_cases.security.dto.account.cambio.CambiarPerfilBaseDTO;
import co.edu.ufps.legal_cases.security.dto.account.cambio.DatosPerfilCambioNormalizados;
import co.edu.ufps.legal_cases.security.model.account.UsuarioSistema;

import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarCodigo;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarEmail;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarNumeroDocumento;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTelefono;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarTexto;
import static co.edu.ufps.legal_cases.common.util.NormalizacionUtils.normalizarUsuario;

// Esto es un servicio auxiliar que normaliza los datos comunes de los perfiles con
// metodos de normalizacion del paquete /common/util
// recibe los datos del usuario en CambiarPerfilBaseDTO y los devuelve normalizados en DatosPerfilCambioNormalizados
@Service
@Transactional(readOnly = true)
public class PerfilCambioDatosService {

    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final SedeRepository sedeRepository;
    private final AreaRepository areaRepository;

    public PerfilCambioDatosService(
            TipoDocumentoRepository tipoDocumentoRepository,
            SedeRepository sedeRepository,
            AreaRepository areaRepository) {
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.sedeRepository = sedeRepository;
        this.areaRepository = areaRepository;
    }

    public DatosPerfilCambioNormalizados normalizarDatosBasicos(
            CambiarPerfilBaseDTO dto,
            UsuarioSistema usuarioSistema,
            boolean documentoObligatorio) {

        if (dto == null) {
            throw new BusinessException("Los datos para el cambio de perfil son obligatorios");
        }

        if (usuarioSistema == null || usuarioSistema.getId() == null) {
            throw new BusinessException("El usuario del sistema es obligatorio");
        }

        String nombre = normalizarTexto(dto.getNombre());
        String documento = documentoObligatorio
                ? normalizarNumeroDocumento(dto.getDocumento())
                : normalizarDocumentoOpcional(dto.getDocumento());
        String email = normalizarEmail(usuarioSistema.getUsername());
        String telefono = normalizarTelefono(dto.getTelefono());
        String usuario = normalizarUsuario(dto.getUsuario());
        String codigo = normalizarCodigo(dto.getCodigo());

        validarTextoObligatorio(nombre, "El nombre es obligatorio");

        if (documentoObligatorio) {
            validarTextoObligatorio(documento, "El documento es obligatorio");
        }

        validarTextoObligatorio(email, "El correo del usuario del sistema es obligatorio");
        validarTextoObligatorio(telefono, "El teléfono es obligatorio");
        validarTextoObligatorio(usuario, "El usuario es obligatorio");
        validarTextoObligatorio(codigo, "El código es obligatorio");

        return new DatosPerfilCambioNormalizados(
                nombre,
                documento,
                email,
                telefono,
                usuario,
                codigo
        );
    }

    // Busca los objetos en la bd por las id y valida
    public TipoDocumento obtenerTipoDocumentoObligatorio(Long tipoDocumentoId) {
        if (tipoDocumentoId == null) {
            throw new BusinessException("El tipo de documento es obligatorio");
        }

        return tipoDocumentoRepository.findById(tipoDocumentoId)
                .orElseThrow(() -> new BusinessException(
                        "Tipo de documento no encontrado con id: " + tipoDocumentoId));
    }

    public TipoDocumento obtenerTipoDocumentoOpcional(Long tipoDocumentoId) {
        if (tipoDocumentoId == null) {
            return null;
        }

        return tipoDocumentoRepository.findById(tipoDocumentoId)
                .orElseThrow(() -> new BusinessException(
                        "Tipo de documento no encontrado con id: " + tipoDocumentoId));
    }

    public Sede obtenerSedeObligatoria(Long sedeId) {
        if (sedeId == null) {
            throw new BusinessException("La sede es obligatoria");
        }

        return sedeRepository.findById(sedeId)
                .orElseThrow(() -> new BusinessException("Sede no encontrada con id: " + sedeId));
    }

    public Sede obtenerSedeOpcional(Long sedeId) {
        if (sedeId == null) {
            return null;
        }

        return sedeRepository.findById(sedeId)
                .orElseThrow(() -> new BusinessException("Sede no encontrada con id: " + sedeId));
    }

    public Area obtenerAreaObligatoria(Long areaId) {
        if (areaId == null) {
            throw new BusinessException("El área es obligatoria");
        }

        return areaRepository.findById(areaId)
                .orElseThrow(() -> new BusinessException("Área no encontrada con id: " + areaId));
    }

    private void validarTextoObligatorio(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new BusinessException(mensaje);
        }
    }

    private String normalizarDocumentoOpcional(String valor) {
        String documento = normalizarNumeroDocumento(valor);

        if (documento == null || documento.isBlank()) {
            return null;
        }

        return documento;
    }
}