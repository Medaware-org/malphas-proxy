# Auth Proxy
Remember to set the following environment variables on deployment:
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`

## Sample config
> Needs to be called `config.yaml` or `config.yml` and placed at the same path as the executable
```yaml
backend:
  host: "http://localhost"
  port: 1234
auth:
  redirect: "http://localhost:5173/dash"
proxy:
  pass: []
frontend:
  origin: "http://localhost:5173"
  ```