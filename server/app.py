import json
import os
import shutil
import subprocess
import uuid
import zipfile
from pathlib import Path
from typing import Dict

from fastapi import FastAPI, UploadFile, File, BackgroundTasks, HTTPException
from fastapi.responses import FileResponse, JSONResponse
from fastapi.staticfiles import StaticFiles

BASE_DIR = Path(__file__).resolve().parent
JOBS_DIR = BASE_DIR / "jobs"
MODELS_DIR = BASE_DIR / "models"
VIEWER_DIR = BASE_DIR.parent / "viewer"

BASE_URL = os.environ.get("BASE_URL", "http://localhost:8000")

app = FastAPI(title="Object Capture Backend")

if VIEWER_DIR.exists():
    app.mount("/viewer", StaticFiles(directory=str(VIEWER_DIR), html=True), name="viewer")


def _status_path(job_id: str) -> Path:
    return JOBS_DIR / job_id / "status.json"


def _write_status(job_id: str, status: str, progress: int, model_url: str | None = None, error: str | None = None) -> None:
    payload: Dict[str, object] = {
        "status": status,
        "progress": progress,
        "model_url": model_url,
        "error": error,
    }
    _status_path(job_id).parent.mkdir(parents=True, exist_ok=True)
    _status_path(job_id).write_text(json.dumps(payload))


def _read_status(job_id: str) -> Dict[str, object]:
    path = _status_path(job_id)
    if not path.exists():
        raise FileNotFoundError
    return json.loads(path.read_text())


def _extract_zip(zip_path: Path, out_dir: Path) -> None:
    if out_dir.exists():
        shutil.rmtree(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(zip_path, "r") as zf:
        zf.extractall(out_dir)


def _run_pipeline(job_id: str, dataset_dir: Path, model_dir: Path) -> None:
    # NOTE: This pipeline expects COLMAP + OpenMVS/Blender installed on the server.
    # Update these commands to match your environment paths.
    _write_status(job_id, "processing", 10)

    # Example: run a shell script if you have one.
    script = BASE_DIR / "run_colmap.sh"
    if script.exists():
        subprocess.run(["bash", str(script), str(dataset_dir)], check=True)

    # Placeholder: you can replace this with actual COLMAP/OpenMVS/Blender calls.
    # If no model is produced, raise to mark job as failed.
    produced = model_dir / "model.glb"
    if not produced.exists():
        raise RuntimeError("No model.glb produced. Configure COLMAP pipeline.")

    _write_status(job_id, "completed", 100, f"{BASE_URL}/model/{job_id}")


@app.post("/upload")
async def upload_dataset(background_tasks: BackgroundTasks, file: UploadFile = File(...)):
    job_id = uuid.uuid4().hex
    job_dir = JOBS_DIR / job_id
    job_dir.mkdir(parents=True, exist_ok=True)

    zip_path = job_dir / "dataset.zip"
    with zip_path.open("wb") as f:
        while True:
            chunk = await file.read(1024 * 1024)
            if not chunk:
                break
            f.write(chunk)

    _write_status(job_id, "processing", 0)

    def process_job():
        try:
            dataset_dir = job_dir / "dataset"
            _extract_zip(zip_path, dataset_dir)
            model_dir = MODELS_DIR / job_id
            model_dir.mkdir(parents=True, exist_ok=True)
            _run_pipeline(job_id, dataset_dir, model_dir)
        except Exception as exc:
            _write_status(job_id, "failed", 100, error=str(exc))

    background_tasks.add_task(process_job)
    return {"job_id": job_id}


@app.get("/status/{job_id}")
def get_status(job_id: str):
    try:
        return JSONResponse(_read_status(job_id))
    except FileNotFoundError:
        raise HTTPException(status_code=404, detail="Job not found")


@app.get("/model/{job_id}")
def get_model(job_id: str):
    model_path = MODELS_DIR / job_id / "model.glb"
    if not model_path.exists():
        raise HTTPException(status_code=404, detail="Model not found")
    return FileResponse(model_path, media_type="model/gltf-binary", filename="model.glb")
