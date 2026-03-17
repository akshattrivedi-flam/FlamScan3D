#!/usr/bin/env bash
set -euo pipefail

DATASET_PATH=${1:-""}
if [[ -z "$DATASET_PATH" ]]; then
  echo "Usage: $0 /path/to/dataset"
  exit 1
fi

IMAGES="$DATASET_PATH/Images"
DB="$DATASET_PATH/database.db"
SPARSE="$DATASET_PATH/sparse"
DENSE="$DATASET_PATH/dense"

mkdir -p "$SPARSE" "$DENSE"

colmap feature_extractor \
  --image_path "$IMAGES" \
  --database_path "$DB" \
  --image_list_path "$DATASET_PATH/Colmap/image_list.txt" \
  --ImageReader.camera_model PINHOLE \
  --ImageReader.camera_params ""

colmap exhaustive_matcher --database_path "$DB"

colmap mapper \
  --database_path "$DB" \
  --image_path "$IMAGES" \
  --output_path "$SPARSE"

colmap image_undistorter \
  --image_path "$IMAGES" \
  --input_path "$SPARSE/0" \
  --output_path "$DENSE" \
  --output_type COLMAP

colmap patch_match_stereo --workspace_path "$DENSE" --workspace_format COLMAP
colmap stereo_fusion --workspace_path "$DENSE" --workspace_format COLMAP --input_type geometric --output_path "$DENSE/fused.ply"
colmap poisson_mesher --input_path "$DENSE/fused.ply" --output_path "$DENSE/mesh.ply"

