// notify-ban — sends the "your account has been locked" email after an admin bans a user.
//
// The ban itself is performed by the admin_set_ban RPC (security definer, is_admin gated);
// this function only delivers the notification, so it re-checks the caller is an admin and
// then reads the target's email with the service role (auth.users is not world-readable).
//
// Delivery is via Gmail SMTP, so a personal Gmail can be the sender without owning a domain.
//
// Deploy:  supabase functions deploy notify-ban
// Secrets: supabase secrets set GMAIL_USER=you@gmail.com GMAIL_APP_PASSWORD=xxxxxxxxxxxxxxxx
//   (SUPABASE_URL / SUPABASE_ANON_KEY / SUPABASE_SERVICE_ROLE_KEY are injected automatically.)
// GMAIL_APP_PASSWORD is a Google "App Password" (16 chars), NOT the account password — it
// requires 2-Step Verification enabled on the Google account. Gmail SMTP allows the `from`
// to be your own address only (no domain verification needed) and caps at ~500 emails/day.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { SMTPClient } from "https://deno.land/x/denomailer@1.6.0/mod.ts";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY")!;
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const GMAIL_USER = Deno.env.get("GMAIL_USER")!;
const GMAIL_APP_PASSWORD = Deno.env.get("GMAIL_APP_PASSWORD")!;
const FROM_NAME = Deno.env.get("GMAIL_FROM_NAME") ?? "Food Tracker";

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

  const client = new SMTPClient({
    connection: {
      hostname: "smtp.gmail.com",
      port: 465,
      tls: true,
      auth: { username: GMAIL_USER, password: GMAIL_APP_PASSWORD },
    },
  });

  try {
    await client.send({
      from: `${FROM_NAME} <${GMAIL_USER}>`,
      to: email,
      subject,
      content: `Tài khoản Food Tracker của bạn đã bị khóa ${lockedFor} do vi phạm quy định cộng đồng.`,
      html,
    });
  } catch (e) {
    return json({ error: "email_send_failed", detail: String(e) }, 502);
  } finally {
    await client.close();
  }

  return json({ ok: true });
});
