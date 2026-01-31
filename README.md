# OpenCode Companion - JetBrains Plugin

A JetBrains IDE plugin that integrates with [OpenCode CLI](https://opencode.ai), allowing you to send selected code directly to OpenCode with context.

## Features

- **Send Code to OpenCode** - Select code and send it with file path and line numbers
- **Templates** - Quick prompts: Explain, Refactor, Write Tests, Fix Bugs, Optimize, Document, Review
- **Agent Selection** - Choose from available agents (build, oracle, explore, librarian, etc.)
- **Session Selection** - Target specific OpenCode sessions
- **TUI Mode** - Append code references to your terminal TUI prompt
- **Auto-Submit** - Optionally auto-submit after appending in TUI mode
- **Server Discovery** - Scan for running OpenCode instances

## Installation

### From ZIP (Manual)

1. Download the latest release: `opencode-jetbrains-plugin-1.0.0.zip`
2. Open your JetBrains IDE (IntelliJ, PyCharm, WebStorm, etc.)
3. Go to **Settings** → **Plugins** → **⚙️** → **Install Plugin from Disk...**
4. Select the ZIP file and restart the IDE

### Build from Source

```bash
# Requires JDK 17
export JAVA_HOME=/path/to/jdk17

# Build the plugin
./gradlew buildPlugin

# Plugin ZIP will be at:
# build/distributions/opencode-jetbrains-plugin-1.0.0.zip
```

## Usage

### Prerequisites

Start OpenCode with a known port:

```bash
opencode --port 4096
```

### Keyboard Shortcuts

| Action | Mac | Windows/Linux |
|--------|-----|---------------|
| Send to OpenCode (with dialog) | `Cmd+Shift+.` | `Ctrl+Shift+.` |
| Quick Send (no dialog) | `Cmd+Alt+.` | `Ctrl+Alt+.` |

### Context Menu

Right-click on selected code → **Send to OpenCode...**

### Dialog Features

- **Template**: Pre-defined prompts (Explain, Refactor, Tests, etc.)
- **Agent**: Select which agent to use
- **Session**: Target a specific session (non-TUI mode)
- **Context**: Add custom instructions
- Press **Enter** to send, **Shift+Enter** for new line

## Configuration

**Settings** → **Tools** → **OpenCode**

| Setting | Description | Default |
|---------|-------------|---------|
| Server URL | OpenCode server address | `http://localhost:4096` |
| Session ID | Target session (blank = auto-detect) | |
| Auto-detect session | Find active session automatically | ✓ |
| Use TUI mode | Append to TUI prompt instead of sending | ✓ |
| Auto-submit (TUI) | Submit automatically after appending | |
| Default Agent | Pre-selected agent in dialog | |

### Server Discovery

Click **Discover Servers** to scan ports 4096-4105 for running OpenCode instances.

### Authentication

If your OpenCode server uses authentication:

```bash
OPENCODE_SERVER_PASSWORD=secret opencode --port 4096
```

Configure username/password in plugin settings.

## Supported IDEs

- IntelliJ IDEA (Community & Ultimate)
- PyCharm
- WebStorm
- PhpStorm
- GoLand
- CLion
- Rider
- RubyMine
- Android Studio

**Requires**: IDE version 2024.1 or later

## Development

```bash
# Run plugin in sandbox IDE for testing
./gradlew runIde

# Build only
./gradlew build

# Build distribution ZIP
./gradlew buildPlugin
```

## License

MIT

## Links

- [OpenCode](https://opencode.ai)
- [OpenCode Documentation](https://opencode.ai/docs/)
- [OpenCode Server API](https://opencode.ai/docs/server/)
