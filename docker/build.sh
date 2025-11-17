#!/bin/bash
# docker buildx build: https://docs.docker.com/engine/reference/commandline/buildx_build/

# Logging functions
error() {
  echo "[$(date +'%Y-%m-%dT%H:%M:%S')] ERROR: $*" >&2
  exit 1
}

warning() {
  echo "[$(date +'%Y-%m-%dT%H:%M:%S')] WARNING: $*" >&1
}

info() {
  echo "[$(date +'%Y-%m-%dT%H:%M:%S')] INFO: $*" >&1
}

# DockerHub Configuration
readonly PUSH_TO_DOCKERHUB=true
readonly DOCKERHUB_REGISTRY="docker.io"
readonly DOCKERHUB_NAMESPACE="viascom"
readonly DOCKERHUB_USERNAME="XXX"
readonly DOCKERHUB_TOKEN="XXX"

# GitHub Container Registry (GHCR) Configuration
readonly PUSH_TO_GHCR=true
readonly GITHUB_REGISTRY="ghcr.io"
readonly GITHUB_NAMESPACE="viascom"
readonly GITHUB_USERNAME="XXX"
readonly GITHUB_TOKEN="XXX"

# Image Metadata
readonly TITLE="Github Maintenance Action"
readonly DESCRIPTION="A GitHub Action for configurable housekeeping of workflow runs, logs, and artifacts."
readonly IMAGE_NAME="github-maintenance-action"
readonly VERSION="0.1.0"
readonly BUILD_DATE=$(date -u +"%Y-%m-%dT%H:%M:%S%z")
readonly AUTHORS="Nikola Stanković <nikola.stankovic@viascom.email>, Patrick Bösch <patrick.boesch@viascom.email>"
readonly IMAGE_URL="https://github.com/viascom/github-maintenance-action/blob/main/README.md"
readonly DOCUMENTATION_URL="https://github.com/viascom/github-maintenance-action/wiki"
readonly SOURCE_URL="https://github.com/viascom/github-maintenance-action.git"
readonly VENDOR="Viascom Ltd liab. Co"
readonly LICENSES="MIT"
readonly REF_NAME=$VERSION
readonly BASE_IMAGE="viascom/ubuntu:24.04.2"
readonly BASE_DIGEST=$(docker inspect --format='{{index .RepoDigests 0}}' "$BASE_IMAGE")
readonly GIT_REVISION=$(git rev-parse HEAD)

# Script Configuration
readonly PULL_PUSHED_IMAGES=true
readonly TEARDOWN_BUILDX=true
readonly INITIAL_BUILDER=$(docker buildx ls | grep '\*' | awk '{print $1}')

# Application Configuration
readonly JAR_FILE_PATH="github-maintenance-action.jar"

setup_buildx() {
  local builder="multiarch-builder"

  if ! docker buildx ls | grep -q "$builder"; then
    docker buildx create --name $builder --use
    docker buildx inspect --bootstrap
  else
    docker buildx use $builder
  fi

  info "Docker buildx setup completed for builder: $builder"
}

teardown_buildx() {
  local builder="multiarch-builder"

  if docker buildx ls | grep -q "$builder"; then
    docker buildx stop $builder
    docker buildx rm $builder
  fi

  docker context use "$INITIAL_BUILDER"

  info "Docker buildx teardown completed for builder: $builder"
}

docker_auth() {
  local action=$1
  local registry=$2
  local username=$3
  local password=$4

  info "$action into $registry with username $username..."
  if [ "$action" == "login" ]; then
    if ! echo "$password" | docker login "$registry" -u "$username" --password-stdin; then
      error "Docker $action to $registry failed! :("
    fi
  elif [ "$action" == "logout" ]; then
    if ! docker logout "$registry"; then
      error "Docker $action to $registry failed! :("
    fi
  fi
}

generate_tags() {
  local registry=$1
  local org=$2
  local image=$3
  local tag=$4

  echo "$registry/$org/$image:$tag"
}

build_image() {
  local tags=("$@")
  local build_args=(
    "--platform" "linux/amd64,linux/arm64"
    "--no-cache"
    "--pull"
    "--build-arg" "JAR_FILE=$JAR_FILE_PATH"
    "--build-arg" "APP_NAME=$IMAGE_NAME"
    "--push"
    "--rm"
  )

  for tag in "${tags[@]}"; do
    build_args+=("--tag" "$tag")
  done

  build_args+=(
    "--label" "org.opencontainers.image.created=${BUILD_DATE:-}"
    "--label" "org.opencontainers.image.authors=${AUTHORS:-}"
    "--label" "org.opencontainers.image.url=${IMAGE_URL:-}"
    "--label" "org.opencontainers.image.documentation=${DOCUMENTATION_URL:-}"
    "--label" "org.opencontainers.image.source=${SOURCE_URL:-}"
    "--label" "org.opencontainers.image.version=${VERSION:-}"
    "--label" "org.opencontainers.image.revision=${GIT_REVISION:-}"
    "--label" "org.opencontainers.image.vendor=${VENDOR:-}"
    "--label" "org.opencontainers.image.licenses=${LICENSES:-}"
    "--label" "org.opencontainers.image.ref.name=${REF_NAME:-}"
    "--label" "org.opencontainers.image.title=${TITLE:-}"
    "--label" "org.opencontainers.image.description=${DESCRIPTION:-}"
    "--label" "org.opencontainers.image.base.digest=${BASE_DIGEST:-}"
    "--label" "org.opencontainers.image.base.name=${BASE_IMAGE:-}"
    "."
  )

  info "Starting Docker buildx build ..."
  setup_buildx

  if ! docker buildx build "${build_args[@]}"; then
    error "Docker buildx build failed! :("
  fi

  info "Docker buildx build completed successfully :)"
}

pull_image() {
  local image=$1
  info "Pulling Docker image: $image..."
  if docker pull "$image"; then
    info "Successfully pulled $image"
  else
    error "Failed to pull $image"
  fi
}

is_semantic_version() {
  local version=$1
  if [[ $version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    return 0
  else
    return 1
  fi
}

main() {
  local major_version=$(echo "$VERSION" | cut -d. -f1)
  local minor_version=$(echo "$VERSION" | cut -d. -f2)
  local patch_version=$(echo "$VERSION" | cut -d. -f3)

  local tags=()

  if is_semantic_version "$VERSION"; then
    tags=("latest" "$major_version" "$major_version.$minor_version" "$VERSION")

    # Add major version tag if it's not 0
    if [[ "$major_version" != "0" ]]; then
      tags+=("$major_version")
    fi

    # Add major.minor tag if minor is not 0
    if [[ "$minor_version" != "0" ]]; then
      tags+=("$major_version.$minor_version")
    fi

    # Add full version tag if patch is not 0
    if [[ "$patch_version" != "0" ]]; then
      tags+=("$VERSION")
    fi
  else
    tags=("$VERSION")
  fi

  if [ "$PUSH_TO_DOCKERHUB" = true ]; then
    docker_auth "login" "$DOCKERHUB_REGISTRY" "$DOCKERHUB_USERNAME" "$DOCKERHUB_TOKEN"
  fi

  if [ "$PUSH_TO_GHCR" = true ]; then
    docker_auth "login" "$GITHUB_REGISTRY" "$GITHUB_USERNAME" "$GITHUB_TOKEN"
  fi

  local full_tags=()
  for tag in "${tags[@]}"; do
    if [ "$PUSH_TO_DOCKERHUB" = true ]; then
      full_tags+=("$(generate_tags "$DOCKERHUB_REGISTRY" "$DOCKERHUB_NAMESPACE" "$IMAGE_NAME" "$tag")")
    fi

    if [ "$PUSH_TO_GHCR" = true ]; then
      full_tags+=("$(generate_tags "$GITHUB_REGISTRY" "$GITHUB_NAMESPACE" "$IMAGE_NAME" "$tag")")
    fi
  done

  info "Full tags, which will be built:"
  for tag in "${full_tags[@]}"; do
    info "- $tag"
  done

  if build_image "${full_tags[@]}"; then
    if [ "$PULL_PUSHED_IMAGES" = true ]; then
      for tag in "${full_tags[@]}"; do
        pull_image "$tag"
      done
    fi
  fi

  if [ "$PUSH_TO_DOCKERHUB" = true ]; then
    docker_auth "logout" "$DOCKERHUB_REGISTRY"
  fi

  if [ "$PUSH_TO_GHCR" = true ]; then
    docker_auth "logout" "$GITHUB_REGISTRY"
  fi

  if [ "$TEARDOWN_BUILDX" = true ]; then
    teardown_buildx
  fi
}

main
