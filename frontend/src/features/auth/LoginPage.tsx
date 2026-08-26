import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, Eye, EyeSlash, ShieldCheck, Storefront } from "@phosphor-icons/react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { z } from "zod";
import { problemMessage } from "../../lib/api/client";
import { useAuthState } from "../../lib/auth/authStore";
import { login, register } from "./authApi";

const schema = z.object({
  email: z.string().trim().email("Enter a valid email address"),
  password: z.string().min(8, "Password must contain at least 8 characters").max(72),
});
type FormValues = z.infer<typeof schema>;

export default function LoginPage() {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [showPassword, setShowPassword] = useState(false);
  const [serverError, setServerError] = useState("");
  const auth = useAuthState();
  const navigate = useNavigate();
  const location = useLocation();
  const { register: field, handleSubmit, formState: { errors, isSubmitting }, reset } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", password: "" },
    mode: "onBlur",
  });

  if (auth.status === "authenticated") return <Navigate to="/dashboard" replace />;

  async function submit(values: FormValues) {
    setServerError("");
    try {
      if (mode === "register") {
        await register(values.email, values.password);
      }
      await login(values.email, values.password);
      const from = (location.state as { from?: string } | null)?.from ?? "/dashboard";
      navigate(from, { replace: true });
    } catch (error) {
      setServerError(problemMessage(error));
    }
  }

  function switchMode(next: "login" | "register") {
    setMode(next);
    setServerError("");
    reset();
  }

  return <main className="auth-page">
    <section className="auth-story" aria-label="RetailHub introduction">
      <div className="auth-story-inner">
        <div className="brand brand-light"><span className="brand-mark"><Storefront size={22} weight="bold" /></span>RetailHub</div>
        <div>
          <p className="eyebrow eyebrow-light">Retail operations, clarified</p>
          <h1>One dependable view of stock, products, and orders.</h1>
          <p>Keep daily retail work moving with a secure workspace built around traceable decisions.</p>
        </div>
        <div className="auth-proof"><ShieldCheck size={24} aria-hidden="true" />
          <span><strong>Session-safe by design</strong>Access tokens stay in memory; rotating refresh tokens remain HttpOnly.</span>
        </div>
      </div>
    </section>
    <section className="auth-form-wrap">
      <div className="auth-form-card">
        <p className="eyebrow">Workspace access</p>
        <h2>{mode === "login" ? "Welcome back" : "Create your account"}</h2>
        <p className="form-intro">{mode === "login" ? "Sign in to continue to RetailHub." : "Start with a secure team member account."}</p>

        <div className="segmented" role="group" aria-label="Authentication mode">
          <button type="button" className={mode === "login" ? "selected" : ""} aria-pressed={mode === "login"}
            onClick={() => switchMode("login")}>Sign in</button>
          <button type="button" className={mode === "register" ? "selected" : ""} aria-pressed={mode === "register"}
            onClick={() => switchMode("register")}>Register</button>
        </div>

        {serverError && <div className="form-alert" role="alert">{serverError}</div>}
        <form onSubmit={handleSubmit(submit)} noValidate>
          <div className="field">
            <label htmlFor="email">Email address</label>
            <input id="email" type="email" autoComplete="email" aria-invalid={!!errors.email}
              aria-describedby={errors.email ? "email-error" : undefined} {...field("email")} />
            {errors.email && <span id="email-error" className="field-error">{errors.email.message}</span>}
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <div className="password-field">
              <input id="password" type={showPassword ? "text" : "password"}
                autoComplete={mode === "login" ? "current-password" : "new-password"}
                aria-invalid={!!errors.password} aria-describedby={errors.password ? "password-error" : undefined}
                {...field("password")} />
              <button type="button" className="password-toggle" aria-label={showPassword ? "Hide password" : "Show password"}
                aria-pressed={showPassword} onClick={() => setShowPassword((shown) => !shown)}>
                {showPassword ? <EyeSlash size={19} /> : <Eye size={19} />}
              </button>
            </div>
            {errors.password && <span id="password-error" className="field-error">{errors.password.message}</span>}
          </div>
          <button className="button button-primary button-full" type="submit" disabled={isSubmitting}>
            {isSubmitting ? <><span className="spinner spinner-small" /> Please wait</> : <>
              {mode === "login" ? "Sign in" : "Create account"}<ArrowRight size={18} aria-hidden="true" />
            </>}
          </button>
        </form>
        <p className="auth-note">By continuing, you use RetailHub's secure cookie-based session flow.</p>
      </div>
    </section>
  </main>;
}

