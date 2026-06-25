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
const SUPPORT_EMAIL = Deno.env.get("SUPPORT_EMAIL") ?? GMAIL_USER;

const TZ = "Asia/Ho_Chi_Minh";

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });

// Human ban window in Vietnam local time, e.g. "Đến 20:20 ngày 25/06/2026" or "Vĩnh viễn".
function banWindowLabel(durationSeconds: number | null | undefined): string {
  if (!durationSeconds || durationSeconds <= 0) return "Vĩnh viễn";
  const until = new Date(Date.now() + durationSeconds * 1000);
  const time = new Intl.DateTimeFormat("vi-VN", {
    timeZone: TZ,
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(until);
  const date = new Intl.DateTimeFormat("vi-VN", {
    timeZone: TZ,
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(until);
  return `Đến ${time} ngày ${date}`;
}

function buildHtml(window: string): string {
  return `<!doctype html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="color-scheme" content="light only">
</head>
<body style="margin:0;padding:0;background-color:#eef2f6;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#eef2f6;padding:28px 14px;font-family:Arial,Helvetica,sans-serif;">
    <tr>
      <td align="center">
        <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="width:100%;max-width:600px;background-color:#ffffff;border-radius:14px;overflow:hidden;border:1px solid #e2e8f0;">
          <tr>
            <td style="background-color:#0f172a;padding:26px 36px;">
              <span style="color:#ffffff;font-size:20px;font-weight:bold;letter-spacing:0.4px;">Food&nbsp;Tracker</span>
            </td>
          </tr>
          <tr>
            <td style="padding:36px 36px 8px 36px;">
              <h1 style="margin:0 0 18px 0;font-size:21px;line-height:1.3;color:#0f172a;">Thông báo khóa tài khoản</h1>
              <p style="margin:0 0 14px 0;font-size:15px;line-height:1.65;color:#334155;">Kính gửi Quý người dùng,</p>
              <p style="margin:0 0 22px 0;font-size:15px;line-height:1.65;color:#334155;">
                Chúng tôi xin thông báo tài khoản Food Tracker của bạn đã bị <strong>tạm khóa</strong> do vi phạm Quy định cộng đồng của ứng dụng.
              </p>
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin:0 0 24px 0;">
                <tr>
                  <td style="background-color:#fef2f2;border-left:4px solid #b91c1c;border-radius:8px;padding:16px 20px;">
                    <div style="font-size:12px;font-weight:bold;letter-spacing:0.6px;text-transform:uppercase;color:#991b1b;margin:0 0 6px 0;">Thời hạn khóa</div>
                    <div style="font-size:18px;font-weight:bold;color:#0f172a;">${window}</div>
                  </td>
                </tr>
              </table>
              <p style="margin:0 0 16px 0;font-size:15px;line-height:1.65;color:#334155;">
                Trong thời gian bị khóa, bạn sẽ không thể đăng nhập hoặc sử dụng các tính năng của ứng dụng.
              </p>
              <p style="margin:0 0 26px 0;font-size:15px;line-height:1.65;color:#334155;">
                Nếu bạn cho rằng đây là sự nhầm lẫn, vui lòng phản hồi tới
                <a href="mailto:${SUPPORT_EMAIL}" style="color:#1d4ed8;text-decoration:none;">${SUPPORT_EMAIL}</a>
                để được xem xét lại.
              </p>
              <p style="margin:0 0 4px 0;font-size:15px;line-height:1.65;color:#334155;">Trân trọng,</p>
              <p style="margin:0 0 8px 0;font-size:15px;line-height:1.65;color:#0f172a;font-weight:bold;">Đội ngũ Food Tracker</p>
            </td>
          </tr>
          <tr>
            <td style="background-color:#f8fafc;border-top:1px solid #e2e8f0;padding:18px 36px;">
              <p style="margin:0;font-size:12px;line-height:1.6;color:#94a3b8;">
                Đây là email tự động, vui lòng không trả lời trực tiếp thư này.<br>
                © Food Tracker. Bảo lưu mọi quyền.
              </p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>`;
}

function buildText(window: string): string {
  return [
    "THÔNG BÁO KHÓA TÀI KHOẢN — FOOD TRACKER",
    "",
    "Kính gửi Quý người dùng,",
    "",
    "Tài khoản Food Tracker của bạn đã bị tạm khóa do vi phạm Quy định cộng đồng.",
    `Thời hạn khóa: ${window}`,
    "",
    "Trong thời gian bị khóa, bạn sẽ không thể đăng nhập hoặc sử dụng ứng dụng.",
    `Nếu cho rằng đây là nhầm lẫn, vui lòng liên hệ: ${SUPPORT_EMAIL}`,
    "",
    "Trân trọng,",
    "Đội ngũ Food Tracker",
  ].join("\n");
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

  const window = banWindowLabel(body.duration_seconds);
  const subject = "Thông báo khóa tài khoản Food Tracker";

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
      content: buildText(window),
      html: buildHtml(window),
    });
  } catch (e) {
    return json({ error: "email_send_failed", detail: String(e) }, 502);
  } finally {
    await client.close();
  }

  return json({ ok: true });
});
