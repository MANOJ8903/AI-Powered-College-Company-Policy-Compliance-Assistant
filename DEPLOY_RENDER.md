Render deployment quick guide

Summary
- Deploy backend (Java) and frontend (static) on Render using the included `render.yaml`.
- Uses embedded H2 by default so you can run on Render free tier without external SQL.

Prerequisites
- Push this repository to GitHub.
- Sign up at https://render.com (free tier).

Steps
1. Push code to GitHub (run from repo root):

```bash
git add .
git commit -m "Prepare render deployment"
# If repo not created yet:
# Create repo on GitHub then:
git remote add origin git@github.com:<your-username>/<your-repo>.git
git branch -M main
git push -u origin main
```

2. Create Render service using `render.yaml` (blueprint)
- In Render dashboard click "New" → "Blueprint" → select your GitHub repo.
- Render will detect `render.yaml` and create two services:
  - `policy-guardian-backend` (type: web)
  - `policy-guardian-frontend` (type: static)
- Review build & start commands, then click "Create Services" / "Deploy".

3. Environment variables
- If you want persistence or use external DB later, set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` in Render service settings.
- For production API key secrets (e.g., `GEMINI_API_KEY`, `JWT_SECRET`), set them in Render's Environment → Secrets.

4. Access
- After deploy, Render will give URLs for both backend and frontend.
- If you used the `render.yaml` default `VITE_API_URL`, frontend will call the correct backend URL.

Notes
- H2 is ephemeral: data resets on redeploy. For persistent DB use Neon or Supabase Postgres and set `DB_URL`.
- To switch to Postgres, update `application.properties` DB variables (we made them configurable via env vars).

Rollback / Logs
- Use Render dashboard to view build logs and service logs. Use `Deploys` to rollback.

If you want, I can now:
- Help push to GitHub from this machine (I will show commands and run them if you confirm credentials).
- Or configure Render steps with exact screenshots and settings.