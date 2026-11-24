param([string]$mensaje = "Actualiza la función chat")

npx supabase functions deploy --project-ref utqtxuxdizxavagckblf --use-api
if ($LASTEXITCODE -ne 0) { Write-Host "Falló el despliegue: no se sube nada." -ForegroundColor Red; exit 1 }

git add .
git commit -m $mensaje
if ($LASTEXITCODE -ne 0) { Write-Host "No había cambios para guardar o falló el commit." -ForegroundColor Yellow; exit 1 }

git push
if ($LASTEXITCODE -ne 0) { Write-Host "Falló la subida a GitHub." -ForegroundColor Red; exit 1 }

Write-Host "Función desplegada y subida a GitHub." -ForegroundColor Green
