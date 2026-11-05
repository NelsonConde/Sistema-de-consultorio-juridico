import { ActivarCuentaForm } from "@/components/auth/ActivarCuentaForm";

export const metadata = {
  title: "Activar cuenta | Consultorio Jurídico",
  description: "Activa tu acceso al Consultorio Jurídico.",
  referrer: "no-referrer",
  robots: {
    index: false,
    follow: false,
  },
};

export default function ActivarCuentaPage() {
  return <ActivarCuentaForm />;
}
