# App auth/onboarding flow

## Email signup
Register → `signUp` (no session; email confirmation ON) → **Verify Email** (6-digit OTP) →
`verifyEmailOtp(SIGNUP)` → `completeOnboarding` → Diary. The account is **inactive until
verified**; cancelling Verify Email leaves it unconfirmed and unable to sign in.
See [email-signup-otp.md](email-signup-otp.md).

## Google sign-in
Google OAuth creates the auth user **and** (via the `handle_new_user` trigger) a profile row
with `user_id = null, onboarding_completed = false` the instant the user picks an account —
this is unavoidable without a service-role key, which must never live in the mobile app.

- **First sign-in** → `getProfileStatus()` = `Incomplete` → **Complete Profile** screen.
- **Cancel** (back arrow / "Hủy" on Complete Profile) → `AuthRepository.signOut()` → the
  MainScaffold session guard routes to **Login** and clears the back stack. The auth user and
  the incomplete profile row are **not** deleted.
- The incomplete row is harmless: hidden by RLS/owner filtering, it isn't a real user
  (`onboarding_completed = false`) and is shown nowhere.
- **Re-login with the same Google account** → still `Incomplete` → back on Complete Profile to
  finish. The trigger's `on conflict (id) do nothing` means no duplicate row is created, so
  finishing yields a single completed profile. This also covers the "quit mid-onboarding" case.

### Future cleanup
Long-abandoned incomplete profiles (`onboarding_completed = false`, old `created_at`) can be
pruned by a future admin-web action using the service-role key. Not built now.

## Change password (in-session)
Settings → "Đổi mật khẩu" → enter current + new + confirm. `AuthRepository.changePassword`
**verifies the current password by re-authenticating** (`signInWith(Email)` with the current
password — wrong password ⇒ `InvalidCredentials`), then `auth.updateUser { password = new }`.
The current password is therefore required and checked client-side.

**Required dashboard setting:** Authentication → Providers/Settings → **"Require current
password when updating" must be OFF.** When ON, GoTrue only accepts password changes that carry
a *reauthentication nonce* (a code emailed via `reauthenticate()`); supabase-kt's `updateUser`
has no field for a typed current password, so a correct typed password is still rejected with
"current password required when setting new password". Turning it OFF lets the in-app flow work
while the app keeps enforcing the current-password check itself. (Keeping it ON would require
adding an emailed-OTP reauthentication step.)
