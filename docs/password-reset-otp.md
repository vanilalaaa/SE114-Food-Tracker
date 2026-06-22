# Password reset via 6-digit email OTP

The in-app reset flow (Forgot password → 3 steps) relies on Supabase delivering a
**6-digit code** in the recovery email instead of a magic link. Two dashboard
settings must be configured **once** by a human — they cannot be set from the app.

## Why

`auth.resetPasswordForEmail(email)` triggers the **Reset Password** email. By
default that template embeds `{{ .ConfirmationURL }}` (a magic link), which needs
a web host to receive the redirect — we have none. Switching the template to send
`{{ .Token }}` makes Supabase deliver the 6-digit OTP that
`auth.verifyEmailOtp(type = OtpType.Email.RECOVERY, ...)` expects.

## Required dashboard steps

1. **Reset Password email template** — Supabase Dashboard → Authentication →
   Email Templates → **Reset Password**. Replace the magic-link body with the
   token, e.g.:

   ```html
   <h2>Đặt lại mật khẩu</h2>
   <p>Mã xác nhận của bạn là:</p>
   <p style="font-size:24px;font-weight:bold;letter-spacing:4px">{{ .Token }}</p>
   <p>Mã có hiệu lực trong 1 giờ.</p>
   ```

   The key change is using `{{ .Token }}` (not `{{ .ConfirmationURL }}`).

2. **Custom SMTP must be active** — Authentication → Settings → SMTP Settings.
   The built-in Supabase mailer only sends to project members and is heavily rate
   limited, so a custom SMTP provider must be enabled and verified, or the reset
   email will not arrive for real users.

## Verification

After both steps:

- Forgot password → enter email → a 6-digit code arrives by email.
- Enter the code → set a new password → sign in with the new password.

If the email never arrives, the blocker is almost always step 2 (SMTP not active)
or the template still pointing at the magic link. This is a **setup blocker**, not
an app bug — the client code is correct once the template sends `{{ .Token }}`.

## Rate limiting

The "Gửi lại mã" (resend) button is gated by a 60-second client cooldown. Supabase
also enforces its own email-send rate limit; exceeding it surfaces in-app as
"Bạn thao tác quá nhanh…" (RateLimited).
