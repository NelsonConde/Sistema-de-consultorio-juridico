import { esAdministrativo, tieneRol } from "@/lib/authz";

export function esRolAdministrador(usuario) {
  return (
    esAdministrativo(usuario) ||
    tieneRol(usuario, "Administrador") ||
    tieneRol(usuario, "Administrativo") ||
    tieneRol(usuario, "Director")
  );
}
