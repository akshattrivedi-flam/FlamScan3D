#!/usr/bin/env bash
set -euo pipefail

DATASET_DIR="$1"
IMAGE_DIR="$DATASET_DIR/Images"
OUT_DIR="$DATASET_DIR/colmap"
DB="$OUT_DIR/database.db"
SPARSE="$OUT_DIR/sparse"
DENSE="$OUT_DIR/dense"

mkdir -p "$OUT_DIR" "$SPARSE" "$DENSE"

# Adjust camera model + params as needed.
colmap feature_extractor \
  --database_path "$DB" \
  --image_path "$IMAGE_DIR" \
  --ImageReader.camera_model PINHOLE

colmap exhaustive_matcher --database_path "$DB"

colmap mapper \
  --database_path "$DB" \
  --image_path "$IMAGE_DIR" \
  --output_path "$SPARSE"

colmap image_undistorter \
  --image_path "$IMAGE_DIR" \
  --input_path "$SPARSE/0" \
  --output_path "$DENSE"

colmap patch_match_stereo --workspace_path "$DENSE"
colmap stereo_fusion --workspace_path "$DENSE" --output_path "$DENSE/fused.ply"

# TODO: Convert fused.ply to model.glb via Blender script.
