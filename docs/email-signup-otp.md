# Email signup activation via 6-digit OTP

A typed-email account is **inactive until the email is verified**. After Register, the
app calls `signUp` (which, with email confirmation enabled, returns **no session**) and
routes to the **Verify Email** screen, which calls
`auth.verifyEmailOtp(type = OtpType.Email.SIGNUP, email, token)`. Only on success does a
session exist; the app then runs `completeOnboarding(...)` and enters the Diary.

This is distinct from password reset, which uses `OtpType.Email.RECOVERY`.

## Required dashboard settings (a human must set these once)

1. **Authentication → Providers → Email → "Confirm email" = ON.**
   This toggle is what enforces activation. If it is **OFF**, `signUp` returns a session
   immediately and Supabase skips the OTP step entirely — the account would be active
   without verification.

2. **Authentication → Email Templates → Confirm signup** must render the **token**, e.g.:

   ```html
   <h2>Xác nhận email</h2>
   <p>Mã xác nhận của bạn là:</p>
   <p style="font-size:24px;font-weight:bold;letter-spacing:4px">{{ .Token }}</p>
   <p>Mã có hiệu lực trong 1 giờ.</p>
   ```

   Use `{{ .Token }}` — **not** `{{ .ConfirmationURL }}`.

   > Clarification: the "8-digit code" seen earlier was the **confirmation-URL hash**, not
   > the OTP. With the template switched to `{{ .Token }}` and **Email OTP length = 6**,
   > the email delivers a clean 6-digit code.

3. **Custom SMTP must be active** (Authentication → Settings → SMTP) or the confirmation
   email will not reach real users.

## Config this app matches

`Email OTP length = 6`, `Email OTP expiration = 3600s`. The Verify Email screen uses a
6-cell `OtpInput`; "Gửi lại mã" has a 60-second client cooldown; expired/invalid codes
map to `auth_err_otp_invalid` ("Mã OTP không đúng hoặc đã hết hạn").

## Cancel behaviour

Cancelling the Verify Email screen (back) returns to Register/Login. The unconfirmed auth
user simply remains unconfirmed — Supabase will not issue a session for it until the email
is verified, so the account stays inactive.
