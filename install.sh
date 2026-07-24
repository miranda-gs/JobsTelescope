#!/usr/bin/env bash
set -euo pipefail

REPO_URL="https://github.com/miranda-gs/JobsTelescope.git"
RELEASES_URL="https://github.com/miranda-gs/JobsTelescope/releases"
JAR_VERSION="0.0.1-SNAPSHOT"
JAR_FILE="JobsTelescope-${JAR_VERSION}.jar"
INSTALL_DIR="${INSTALL_DIR:-$HOME/JobsTelescope}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log()   { printf "${GREEN}==>${NC} %s\n" "$1"; }
warn()  { printf "${YELLOW}==>${NC} %s\n" "$1"; }
error() { printf "${RED}==>${NC} %s\n" "$1"; }
info()  { printf "${CYAN}  ->${NC} %s\n" "$1"; }

cleanup() {
  local exit_code=$?
  if [ $exit_code -ne 0 ]; then
    error "Installation failed at step: ${CURRENT_STEP:-unknown}"
  fi
  exit $exit_code
}
trap cleanup EXIT

detect_os() {
  case "$(uname -s)" in
    Linux*)  OS="linux" ;;
    Darwin*) OS="macos" ;;
    *)       error "Unsupported OS: $(uname -s)"; exit 1 ;;
  esac
  log "Detected OS: ${OS}"

  ARCH=$(uname -m)
  case "$ARCH" in
    x86_64|amd64) ARCH="x64" ;;
    aarch64|arm64) ARCH="aarch64" ;;
    *) error "Unsupported architecture: ${ARCH}"; exit 1 ;;
  esac
  info "Architecture: ${ARCH}"
}

detect_distro() {
  if [ "$OS" = "macos" ]; then
    if command -v brew &>/dev/null; then
      PACKAGE_MANAGER="brew"
      INSTALL_CMD="brew install"
    else
      warn "Homebrew not found. Installing Homebrew first..."
      /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
      PACKAGE_MANAGER="brew"
      INSTALL_CMD="brew install"
    fi
    return
  fi

  if [ -f /etc/os-release ]; then
    . /etc/os-release
    DISTRO="${ID:-unknown}"
  elif command -v lsb_release &>/dev/null; then
    DISTRO=$(lsb_release -si 2>/dev/null | tr '[:upper:]' '[:lower:]')
  else
    DISTRO="unknown"
  fi

  case "$DISTRO" in
    ubuntu|debian|linuxmint|pop)
      PACKAGE_MANAGER="apt"
      INSTALL_CMD="sudo apt install -y" ;;
    fedora|rhel|centos)
      PACKAGE_MANAGER="dnf"
      INSTALL_CMD="sudo dnf install -y" ;;
    arch|manjaro|endeavouros)
      PACKAGE_MANAGER="pacman"
      INSTALL_CMD="sudo pacman -S --noconfirm" ;;
    alpine)
      PACKAGE_MANAGER="apk"
      INSTALL_CMD="sudo apk add" ;;
    opensuse*|suse)
      PACKAGE_MANAGER="zypper"
      INSTALL_CMD="sudo zypper install -y" ;;
    *)
      warn "Unknown distro: ${DISTRO}. Attempting apt..."
      PACKAGE_MANAGER="apt"
      INSTALL_CMD="sudo apt install -y" ;;
  esac

  log "Detected distro: ${DISTRO:-unknown} (${PACKAGE_MANAGER})"
}

check_java() {
  CURRENT_STEP="check_java"

  if ! command -v java &>/dev/null; then
    return 1
  fi

  local raw version
  raw=$(java -version 2>&1 | head -1)

  case "$raw" in
    *GraalVM*) info "GraalVM detected" ;;
  esac

  version=$(echo "$raw" | sed 's/[^0-9.]//g' | cut -d. -f1)

  if [ "$version" -lt 25 ] 2>/dev/null; then
    info "Java version ${version} is too old. Need Java 25+ (GraalVM recommended)."
    return 1
  fi

  log "Java ${version} found (JRE is sufficient)"
  return 0
}

install_java_graalvm() {
  CURRENT_STEP="install_java_graalvm"
  log "Installing GraalVM JDK 25 (includes JRE)..."

  local graal_url graal_archive graal_dir
  case "$OS" in
    linux)
      graal_url="https://download.oracle.com/graalvm/25/latest/graalvm-jdk-25_linux-${ARCH}_bin.tar.gz"
      graal_archive="$HOME/graalvm.tar.gz"
      graal_dir="$HOME/graalvm-jdk-25"
      ;;
    macos)
      graal_url="https://download.oracle.com/graalvm/25/latest/graalvm-jdk-25_macos-${ARCH}_bin.tar.gz"
      graal_archive="$HOME/graalvm.tar.gz"
      graal_dir="$HOME/graalvm-jdk-25"
      ;;
  esac

  info "Downloading GraalVM 25..."
  if command -v curl &>/dev/null; then
    curl -fsSL "$graal_url" -o "$graal_archive"
  elif command -v wget &>/dev/null; then
    wget -q "$graal_url" -O "$graal_archive"
  else
    error "Neither curl nor wget found. Install one and re-run."
    exit 1
  fi

  info "Extracting..."
  mkdir -p "$graal_dir"
  tar -xzf "$graal_archive" -C "$graal_dir" --strip-components=1

  local java_bin="$graal_dir/bin/java"
  if [ ! -f "$java_bin" ]; then
    error "GraalVM extraction failed."
    exit 1
  fi

  "$java_bin" -version 2>&1 | head -1

  cat <<EOF | sudo tee "$HOME/.graalvm-path.sh" >/dev/null
export PATH="$graal_dir/bin:\$PATH"
export JAVA_HOME="$graal_dir"
EOF

  export PATH="$graal_dir/bin:$PATH"
  export JAVA_HOME="$graal_dir"

  case "$SHELL" in
    *zsh)
      if ! grep -q "graalvm-path" "$HOME/.zshrc" 2>/dev/null; then
        echo ". \$HOME/.graalvm-path.sh" >> "$HOME/.zshrc"
      fi ;;
    *bash)
      if ! grep -q "graalvm-path" "$HOME/.bashrc" 2>/dev/null; then
        echo ". \$HOME/.graalvm-path.sh" >> "$HOME/.bashrc"
      fi ;;
  esac

  log "GraalVM 25 installed and added to PATH"
}

install_java() {
  CURRENT_STEP="install_java"
  if check_java; then return; fi

  log "Installing Java 25+ JRE..."

  case "${PACKAGE_MANAGER:-}" in
    apt)
      sudo apt update -qq
      $INSTALL_CMD openjdk-25-jre 2>/dev/null ||
        $INSTALL_CMD openjdk-21-jre 2>/dev/null ||
        install_java_graalvm ;;
    dnf)
      $INSTALL_CMD java-25-openjdk 2>/dev/null ||
        $INSTALL_CMD java-21-openjdk 2>/dev/null ||
        install_java_graalvm ;;
    pacman)
      $INSTALL_CMD jre21-openjdk 2>/dev/null ||
        install_java_graalvm ;;
    brew)
      brew install openjdk@21 2>/dev/null ||
        install_java_graalvm ;;
    *)
      install_java_graalvm ;;
  esac

  if ! check_java; then
    install_java_graalvm
  fi

  log "Java JRE installed successfully"
}

check_node() {
  CURRENT_STEP="check_node"
  if command -v node &>/dev/null; then
    local version
    version=$(node -v | sed 's/v//' | cut -d. -f1)
    if [ "$version" -ge 18 ] 2>/dev/null; then
      log "Node.js $(node -v) found"
      return 0
    fi
    info "Node.js version $(node -v) is too old. Need 18+."
  fi
  return 1
}

install_node() {
  CURRENT_STEP="install_node"
  if check_node; then return; fi

  log "Installing Node.js 20+..."

  case "${PACKAGE_MANAGER:-}" in
    apt)
      curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
      $INSTALL_CMD nodejs ;;
    dnf)
      curl -fsSL https://rpm.nodesource.com/setup_22.x | sudo -E bash -
      $INSTALL_CMD nodejs ;;
    pacman)
      $INSTALL_CMD nodejs npm ;;
    apk)
      $INSTALL_CMD nodejs npm ;;
    brew)
      brew install node@22
      brew link --overwrite node@22 ;;
    zypper)
      $INSTALL_CMD nodejs22 ;;
    *)
      warn "Installing via nvm instead..."
      curl -fsSL https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.0/install.sh | bash
      export NVM_DIR="$HOME/.nvm"
      [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
      nvm install 22
      nvm use 22 ;;
  esac

  if ! check_node; then
    error "Node.js installation failed. Install Node.js 18+ manually."
    exit 1
  fi
  log "Node.js installed successfully"
}

install_git() {
  CURRENT_STEP="install_git"
  if command -v git &>/dev/null; then
    log "Git found"
    return
  fi
  log "Installing Git..."
  case "${PACKAGE_MANAGER:-}" in
    brew) brew install git ;;
    pacman) $INSTALL_CMD git ;;
    apk) $INSTALL_CMD git ;;
    *) $INSTALL_CMD git ;;
  esac
}

download_jar() {
  CURRENT_STEP="download_jar"
  local jar_dir="$INSTALL_DIR/target"
  local jar_path="$jar_dir/$JAR_FILE"

  mkdir -p "$jar_dir"

  if [ -f "$jar_path" ]; then
    log "JAR already exists at ${jar_path}"
    return
  fi

  local jar_url="$RELEASES_URL/download/v${JAR_VERSION}/${JAR_FILE}"
  log "Downloading pre-built JAR from GitHub releases..."

  local http_code
  if command -v curl &>/dev/null; then
    http_code=$(curl -fsL -o "$jar_path" -w "%{http_code}" "$jar_url" 2>/dev/null || echo "000")
  elif command -v wget &>/dev/null; then
    http_code=$(wget -q "$jar_url" -O "$jar_path" 2>&1 && echo "200" || echo "000")
  else
    error "Neither curl nor wget found."
    exit 1
  fi

  if [ "$http_code" = "200" ]; then
    log "JAR downloaded successfully"
  else
    rm -f "$jar_path"
    warn "Pre-built JAR not found at ${jar_url}"
    warn "The release may not be published yet."
    info "Build the backend yourself with JDK:"
    info "  cd ${INSTALL_DIR} && ./mvnw package -DskipTests"
    info "Or place your JAR at: ${jar_path}"
    exit 1
  fi
}

setup_repo() {
  CURRENT_STEP="setup_repo"

  if [ -d "$INSTALL_DIR" ] && [ -f "$INSTALL_DIR/ui/package.json" ]; then
    log "Repository already exists at ${INSTALL_DIR}"
    cd "$INSTALL_DIR"
    return
  fi

  log "Cloning repository..."
  git clone "$REPO_URL" "$INSTALL_DIR"
  cd "$INSTALL_DIR"
}

install_ui_deps() {
  CURRENT_STEP="install_ui_deps"
  log "Installing UI dependencies..."
  cd "$INSTALL_DIR/ui"
  npm install
  log "UI dependencies installed"
}

setup_global_command() {
  CURRENT_STEP="setup_global_command"
  log "Registering jtelescope as a global command..."

  cd "$INSTALL_DIR/ui"

  if npm link &>/dev/null; then
    log "jtelescope command registered globally"
    return
  fi

  warn "npm link failed (may need permissions)."
  info "Try running manually:"
  info "  cd ${INSTALL_DIR}/ui && sudo npm link"
  info ""
  info "Alternatively, add this to your ~/.bashrc or ~/.zshrc:"
  info "  export PATH=\"${INSTALL_DIR}/ui/bin:\$PATH\""
}

show_completion() {
  cat <<EOF

$(log "Installation complete!")
$(info "Run ${GREEN}jtelescope${NC} from anywhere in your terminal.")
$(info "")
$(info "With a custom JAR path:")
$(info "  jtelescope /path/to/jar")
$(info "")
$(info "Required: Java 25+ JRE (any distribution)")
$(info "Optional for building from source: JDK 25+ with javac")
EOF
}

main() {
  echo ""
  info "Jobs Telescope - Cross-Platform Installer"
  info "=========================================="
  echo ""

  CURRENT_STEP="detection"
  detect_os
  detect_distro

  CURRENT_STEP="dependencies"
  install_git
  install_java
  install_node

  CURRENT_STEP="setup"
  setup_repo
  download_jar
  install_ui_deps
  setup_global_command

  show_completion
}

main "$@"
