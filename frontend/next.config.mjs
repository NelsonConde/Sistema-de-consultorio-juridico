/** @type {import('next').NextConfig} */
const nextConfig = {
  reactCompiler: true,
  async rewrites() {
    const value = process.env.API_PROXY_ORIGIN?.trim();
    if (!value) return [];

    let upstream;
    try {
      upstream = new URL(value);
    } catch {
      throw new Error('API_PROXY_ORIGIN debe contener un origen HTTP(S) válido');
    }

    const localHost = ['localhost', '127.0.0.1'].includes(upstream.hostname);
    const localHttp = process.env.NODE_ENV !== 'production' &&
      localHost && upstream.protocol === 'http:';

    if (
      (upstream.protocol !== 'https:' && !localHttp) ||
      upstream.username || upstream.password ||
      upstream.pathname !== '/' || upstream.search || upstream.hash
    ) {
      throw new Error('API_PROXY_ORIGIN debe ser un origen HTTPS sin ruta ni credenciales');
    }

    return [{
      source: '/api/:path*',
      destination: `${upstream.origin}/api/:path*`,
    }];
  },
};

export default nextConfig;
