# Object Capture Backend

## Setup
```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
export BASE_URL=http://localhost:8000
uvicorn app:app --host 0.0.0.0 --port 8000
```

## Endpoints
- `POST /upload` → returns `{ "job_id": "..." }`
- `GET /status/{job_id}` → `{ status, progress, model_url }`
- `GET /model/{job_id}` → serves `model.glb`
- `GET /viewer` → static Three.js viewer (if `viewer/` exists)

## Pipeline
`run_colmap.sh` is a placeholder pipeline. Install COLMAP and update paths. After producing `model.glb`, place it under `server/models/{job_id}/model.glb`.
