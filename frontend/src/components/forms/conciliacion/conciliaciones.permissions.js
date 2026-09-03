import { esAdministrativo } from "@/lib/authz";

export function esRolAdministrador(usuario) {
  return esAdministrativo(usuario);
}
