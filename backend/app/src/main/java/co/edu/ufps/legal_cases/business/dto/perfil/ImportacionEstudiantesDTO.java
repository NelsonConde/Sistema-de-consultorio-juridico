package co.edu.ufps.legal_cases.business.dto.perfil;

import java.util.ArrayList;
import java.util.List;

public class ImportacionEstudiantesDTO {

    private int totalFilas;

    private int exitosos;

    private int fallidos;

    private List<String> errores =
            new ArrayList<>();

    public int getTotalFilas() {
        return totalFilas;
    }

    public void setTotalFilas(int totalFilas) {
        this.totalFilas = totalFilas;
    }

    public int getExitosos() {
        return exitosos;
    }

    public void setExitosos(int exitosos) {
        this.exitosos = exitosos;
    }

    public int getFallidos() {
        return fallidos;
    }

    public void setFallidos(int fallidos) {
        this.fallidos = fallidos;
    }

    public List<String> getErrores() {
        return errores;
    }

    public void setErrores(List<String> errores) {
        this.errores = errores;
    }
}