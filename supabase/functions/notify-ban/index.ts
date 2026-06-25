// notify-ban — sends the "your account has been locked" email after an admin bans a user.
//
// The ban itself is performed by the admin_set_ban RPC (security definer, is_admin gated);
// this function only delivers the notification, so it re-checks the caller is an admin and
// then reads the target's email with the service role (auth.users is not world-readable).
//
// Deploy:  supabase functions deploy notify-ban
// Secrets: supabase secrets set RESEND_API_KEY=...  BAN_EMAIL_FROM="Food Tracker <no-reply@your-domain>"
//   (SUPABASE_URL / SUPABASE_ANON_KEY / SUPABASE_SERVICE_ROLE_KEY are injected automatically.)
// RESEND_API_KEY must come from a verified Resend domain or sends will be rejected.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY")!;
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const RESEND_API_KEY = Deno.env.get("RESEND_API_KEY")!;
const BAN_EMAIL_FROM = Deno.env.get("BAN_EMAIL_FROM") ?? "Food Tracker <onboarding@resend.dev>";

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });

// now + duration in Vietnam local time, or "vĩnh viễn" for a permanent ban.
function durationText(durationSeconds: number | null | undefined): string {
  if (!durationSeconds || durationSeconds <= 0) return "vĩnh viễn";
  const until = new Date(Date.now() + durationSeconds * 1000);
  const formatted = until.toLocaleString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" });
  return `đến ${formatted}`;
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  // Re-check the caller is an admin, using their forwarded JWT.
  const authHeader = req.headers.get("Authorization") ?? "";
  const caller = createClient(SUPABASE_URL, ANON_KEY, {
    global: { headers: { Authorization: authHeader } },
  });
  const { data: isAdmin, error: adminErr } = await caller.rpc("am_i_admin");
  if (adminErr || isAdmin !== true) return json({ error: "not_authorized" }, 403);

  let body: { target_id?: string; duration_seconds?: number | null };
  try {
    body = await req.json();
  } catch {
    return json({ error: "invalid_body" }, 400);
  }
  const targetId = body.target_id;
  if (!targetId) return json({ error: "missing_target_id" }, 400);

  // Read the target's email with the service role (auth.users is not exposed to clients).
  const admin = createClient(SUPABASE_URL, SERVICE_ROLE_KEY);
  const { data: target, error: lookupErr } = await admin.auth.admin.getUserById(targetId);
  const email = target?.user?.email;
  if (lookupErr || !email) return json({ error: "target_email_not_found" }, 404);

  const lockedFor = durationText(body.duration_seconds);
  const subject = "Tài khoản Food Tracker của bạn đã bị khóa";
  const html =
    `<p>Xin chào,</p>` +
    `<p>Tài khoản Food Tracker của bạn đã bị khóa <strong>${lockedFor}</strong> do vi phạm quy định cộng đồng.</p>` +
    `<p>Trong thời gian bị khóa bạn sẽ không thể đăng nhập. Nếu cho rằng đây là nhầm lẫn, vui lòng liên hệ bộ phận hỗ trợ.</p>` +
    `<p>— Đội ngũ Food Tracker</p>`;

  const resp = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${RESEND_API_KEY}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ from: BAN_EMAIL_FROM, to: email, subject, html }),
  });

  if (!resp.ok) {
    const detail = await resp.text();
    return json({ error: "email_send_failed", detail }, 502);
  }
  return json({ ok: true });
});
