import { ThemeProvider } from "@/components/theme-provider";
import "./globals.css";

export const metadata = {
  title: "Consultorio Jurídico",
  description: "Sistema de gestión de casos jurídicos del consultorio jurídico",
};

export default function RootLayout({ children }) {
  return (
    <html lang="es" suppressHydrationWarning>
      <body className="min-h-full">
        <ThemeProvider
          attribute="class"
          defaultTheme="system"
          enableSystem
          disableTransitionOnChange
        >
          {children}
        </ThemeProvider>
      </body>
    </html>
  );
}