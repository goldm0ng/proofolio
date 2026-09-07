import type { NextRequest } from "next/server";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";
const TOKEN = process.env.APP_API_TOKEN ?? "";

type Ctx = { params: Promise<{ path: string[] }> };

async function proxy(req: NextRequest, ctx: Ctx): Promise<Response> {
  const { path } = await ctx.params;
  const target = `${BACKEND_URL}/api/v1/${path.join("/")}${req.nextUrl.search}`;

  const headers = new Headers();
  headers.set("Authorization", `Bearer ${TOKEN}`);
  const contentType = req.headers.get("content-type");
  if (contentType) headers.set("content-type", contentType);
  headers.set("accept", req.headers.get("accept") ?? "application/json");

  const hasBody = req.method !== "GET" && req.method !== "HEAD";
  let upstream: Response;
  try {
    upstream = await fetch(target, {
      method: req.method,
      headers,
      body: hasBody ? await req.arrayBuffer() : undefined,
      cache: "no-store",
      redirect: "manual",
    });
  } catch {
    return Response.json(
      { code: "BACKEND_UNAVAILABLE", message: "백엔드에 연결할 수 없습니다. 서버가 실행 중인지 확인하세요." },
      { status: 503 },
    );
  }

  const resHeaders = new Headers();
  const upstreamType = upstream.headers.get("content-type");
  if (upstreamType) resHeaders.set("content-type", upstreamType);
  resHeaders.set("cache-control", "no-store");

  if (upstream.status === 204) return new Response(null, { status: 204, headers: resHeaders });
  return new Response(upstream.body, { status: upstream.status, headers: resHeaders });
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
