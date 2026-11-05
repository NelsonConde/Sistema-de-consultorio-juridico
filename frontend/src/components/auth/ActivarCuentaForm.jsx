"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { useTheme } from "next-themes";
import {
  AlertCircle,
  CheckCircle2,
  Eye,
  EyeOff,
  Moon,
  Scale,
  ShieldCheck,
  Sun,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  AccountActivationError,
  ACTIVATION_FAILURE,
  activateAccount,
} from "@/lib/account-activation";
import { passwordPolicyRule, passwordsMatchRule } from "@/lib/form-validation";

const INITIALIZING = "initializing";
const READY = "ready";
const SUCCESS = "success";

const FAILURE_CONTENT = {
  [ACTIVATION_FAILURE.EXPIRED]: {
    title: "El enlace venció",
    description:
      "Solicita una nueva invitación para completar la activación de tu cuenta.",
  },
  [ACTIVATION_FAILURE.INVALID]: {
    title: "El enlace no es válido",
    description:
      "El enlace está incompleto o ya no está disponible. Solicita una nueva invitación.",
  },
  [ACTIVATION_FAILURE.REVOKED]: {
    title: "El enlace ya no está disponible",
    description:
      "Solicita una nueva invitación para completar la activación de tu cuenta.",
  },
  [ACTIVATION_FAILURE.USED]: {
    title: "El enlace ya fue utilizado",
    description:
      "Puedes iniciar sesión o solicitar ayuda si todavía no tienes acceso.",
  },
};

function PasswordInput({ error, id, label, registration, visible, onToggle }) {
  const errorId = `${id}-error`;
  const helpId = id === "passwordNueva" ? `${id}-help` : null;
  const describedBy = [helpId, error ? errorId : null]
    .filter(Boolean)
    .join(" ");

  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <div className="relative">
        <Input
          {...registration}
          id={id}
          type={visible ? "text" : "password"}
          autoComplete="new-password"
          aria-invalid={Boolean(error)}
          aria-describedby={describedBy || undefined}
          className="h-10 pr-11"
        />
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="absolute right-1 top-1 h-8 w-8"
          onClick={onToggle}
          aria-label={`${visible ? "Ocultar" : "Mostrar"} ${label.toLowerCase()}`}
          aria-pressed={visible}
        >
          {visible ? <EyeOff aria-hidden="true" /> : <Eye aria-hidden="true" />}
        </Button>
      </div>
      {helpId ? (
        <p id={helpId} className="text-xs leading-5 text-muted-foreground">
          Usa entre 8 y 100 caracteres, una mayúscula y un número.
        </p>
      ) : null}
      {error ? (
        <p id={errorId} className="text-sm text-destructive" role="alert">
          {error.message}
        </p>
      ) : null}
    </div>
  );
}

function ThemeMenu() {
  const { setTheme } = useTheme();

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          type="button"
          variant="outline"
          size="icon"
          aria-label="Cambiar tema visual"
        >
          <Sun className="transition-all dark:scale-0 dark:-rotate-90" />
          <Moon className="absolute scale-0 rotate-90 transition-all dark:scale-100 dark:rotate-0" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuItem onClick={() => setTheme("light")}>
          Claro
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => setTheme("dark")}>
          Oscuro
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => setTheme("system")}>
          Sistema
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

function TerminalState({ state }) {
  const content = FAILURE_CONTENT[state];

  if (state === SUCCESS) {
    return (
      <Card className="w-full max-w-md shadow-lg">
        <CardHeader className="text-center">
          <CheckCircle2
            className="mx-auto mb-2 h-11 w-11 text-emerald-600 dark:text-emerald-400"
            aria-hidden="true"
          />
          <CardTitle className="text-2xl">Cuenta activada</CardTitle>
          <CardDescription className="leading-6">
            Tu contraseña se creó correctamente. Ya puedes iniciar sesión.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Button asChild className="h-10 w-full">
            <Link href="/">Ir al inicio de sesión</Link>
          </Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="w-full max-w-md shadow-lg">
      <CardHeader className="text-center">
        <AlertCircle
          className="mx-auto mb-2 h-11 w-11 text-amber-600 dark:text-amber-400"
          aria-hidden="true"
        />
        <CardTitle className="text-2xl">{content.title}</CardTitle>
        <CardDescription className="leading-6">
          {content.description}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Button asChild variant="outline" className="h-10 w-full">
          <Link href="/">Ir al inicio de sesión</Link>
        </Button>
      </CardContent>
    </Card>
  );
}

export function ActivarCuentaForm() {
  const tokenRef = useRef(null);
  const submittingRef = useRef(false);
  const statusRef = useRef(null);
  const [flowState, setFlowState] = useState(INITIALIZING);
  const [requestError, setRequestError] = useState("");
  const [correlationId, setCorrelationId] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmation, setShowConfirmation] = useState(false);

  const {
    register,
    handleSubmit,
    getValues,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    defaultValues: {
      passwordNueva: "",
      confirmarPassword: "",
    },
    mode: "onBlur",
  });


  useEffect(() => {
    const currentUrl = new URL(window.location.href);

    const token =
        tokenRef.current ??
        currentUrl.searchParams.get("token")?.trim() ??
        null;

    tokenRef.current = token;
    window.history.replaceState(
        window.history.state,
        "",
        currentUrl.pathname,
    );

    setFlowState(token ? READY : ACTIVATION_FAILURE.INVALID);

    return () => {
      submittingRef.current = false;
    };
  }, []);


  useEffect(() => {
    if (flowState !== INITIALIZING) {
      statusRef.current?.focus();
    }
  }, [flowState]);

  const onSubmit = async ({ passwordNueva }) => {
    if (submittingRef.current || !tokenRef.current) return;

    submittingRef.current = true;
    setRequestError("");
    setCorrelationId("");

    try {
      await activateAccount({
        token: tokenRef.current,
        password: passwordNueva,
      });
      tokenRef.current = null;
      reset();
      setShowPassword(false);
      setShowConfirmation(false);
      setFlowState(SUCCESS);
    } catch (error) {
      if (
        error instanceof AccountActivationError &&
        error.reason !== ACTIVATION_FAILURE.UNAVAILABLE
      ) {
        tokenRef.current = null;
        reset();
        setFlowState(error.reason);
        return;
      }

      setRequestError(
        "No pudimos completar la activación. Verifica tu conexión e intenta nuevamente.",
      );
      setCorrelationId(error?.correlationId || "");
    } finally {
      submittingRef.current = false;
    }
  };

  const isBusy = isSubmitting || submittingRef.current;

  return (
    <main className="relative flex min-h-screen overflow-x-hidden bg-background">
      <div className="absolute right-4 top-4 z-20">
        <ThemeMenu />
      </div>

      <section className="hidden min-h-screen w-1/2 flex-col justify-between bg-gradient-to-b from-blue-600 via-indigo-600 to-indigo-800 p-12 text-white lg:flex">
        <div className="flex items-center gap-3">
          <Scale className="h-8 w-8" aria-hidden="true" />
          <span className="text-xl font-semibold">Consultorio Jurídico</span>
        </div>

        <div className="max-w-md space-y-4">
          <ShieldCheck className="h-12 w-12" aria-hidden="true" />
          <h1 className="text-4xl font-bold leading-tight">
            Activa tu acceso de forma segura
          </h1>
          <p className="text-white/80">
            Crea tu contraseña para comenzar a utilizar la plataforma.
          </p>
        </div>

        <p className="text-sm text-white/70">
          © 2026 Todos los derechos reservados
        </p>
      </section>

      <section className="flex min-h-screen w-full items-center justify-center px-4 py-20 sm:px-6 lg:w-1/2">
        <div
          ref={statusRef}
          tabIndex={-1}
          className="w-full max-w-md outline-none"
        >
          {flowState === INITIALIZING ? (
            <Card className="w-full shadow-lg" aria-live="polite">
              <CardContent className="py-8 text-center text-muted-foreground">
                Verificando enlace…
              </CardContent>
            </Card>
          ) : flowState !== READY ? (
            <TerminalState state={flowState} />
          ) : (
            <Card className="w-full shadow-lg">
              <CardHeader>
                <div className="mb-2 flex items-center gap-2 text-primary lg:hidden">
                  <Scale className="h-6 w-6" aria-hidden="true" />
                  <span className="font-semibold">Consultorio Jurídico</span>
                </div>
                <CardTitle className="text-2xl">Activar cuenta</CardTitle>
                <CardDescription className="leading-6">
                  Define una contraseña para completar la activación.
                </CardDescription>
              </CardHeader>

              <CardContent>
                <form
                  className="space-y-5"
                  onSubmit={handleSubmit(onSubmit)}
                  noValidate
                >
                  <PasswordInput
                    id="passwordNueva"
                    label="Nueva contraseña"
                    registration={register(
                      "passwordNueva",
                      passwordPolicyRule(),
                    )}
                    error={errors.passwordNueva}
                    visible={showPassword}
                    onToggle={() => setShowPassword((visible) => !visible)}
                  />

                  <PasswordInput
                    id="confirmarPassword"
                    label="Confirmar nueva contraseña"
                    registration={register(
                      "confirmarPassword",
                      passwordsMatchRule(() => getValues("passwordNueva")),
                    )}
                    error={errors.confirmarPassword}
                    visible={showConfirmation}
                    onToggle={() => setShowConfirmation((visible) => !visible)}
                  />

                  {requestError ? (
                    <div
                      className="rounded-lg border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
                      role="alert"
                    >
                      <p>{requestError}</p>
                      {correlationId ? (
                        <p className="mt-1">Referencia: {correlationId}</p>
                      ) : null}
                    </div>
                  ) : null}

                  <Button
                    type="submit"
                    className="h-10 w-full"
                    disabled={isBusy}
                  >
                    {isBusy ? "Activando…" : "Activar cuenta"}
                  </Button>
                </form>
              </CardContent>
            </Card>
          )}
        </div>
      </section>
    </main>
  );
}
