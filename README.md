# OpenCode Companion - JetBrains Plugin

[![JetBrains Marketplace](https://img.shields.io/jetbrains/plugin/v/eu.devmoon.opencode-companion.svg)](https://plugins.jetbrains.com/plugin/eu.devmoon.opencode-companion)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/eu.devmoon.opencode-companion.svg)](https://plugins.jetbrains.com/plugin/eu.devmoon.opencode-companion)

A JetBrains IDE plugin that integrates with [OpenCode CLI](https://opencode.ai), allowing you to send selected code directly to OpenCode with context.

**Developed by [devmoon](https://devmoon.eu)**

## Features

- **Send Code to OpenCode** - Select code and send it with file path and line numbers
- **Templates** - Quick prompts: Explain, Refactor, Write Tests, Fix Bugs, Optimize, Document, Review
- **Agent Selection** - Choose from available agents (build, oracle, explore, librarian, etc.)
- **Session Selection** - Target specific OpenCode sessions
- **TUI Mode** - Append code references to your terminal TUI prompt
- **Auto-Submit** - Optionally auto-submit after appending in TUI mode
- **Server Discovery** - Scan for running OpenCode instances

## Installation

### From JetBrains Marketplace (Recommended)

1. Open your JetBrains IDE (IntelliJ, PyCharm, WebStorm, etc.)
2. Go to **Settings** → **Plugins** → **Marketplace**
3. Search for **"OpenCode Companion"**
4. Click **Install** and restart the IDE

Or install directly from: [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/eu.devmoon.opencode-companion)

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

**Settings** → **Tools** → **OpenCode Companion**

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

### Build from Source

```bash
# Requires JDK 17
export JAVA_HOME=/path/to/jdk17

# Build the plugin
./gradlew buildPlugin

# Plugin ZIP will be at: build/distributions/opencode-jetbrains-plugin-1.0.0.zip
```

### Run in Sandbox IDE

```bash
./gradlew runIde
```

## License

MIT License - see [LICENSE](LICENSE) for details.

## Links

- [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/eu.devmoon.opencode-companion)
- [OpenCode](https://opencode.ai)
- [OpenCode Documentation](https://opencode.ai/docs/)
- [devmoon](https://devmoon.eu)

## Support

For issues and feature requests, please contact [support@devmoon.eu](mailto:support@devmoon.eu).
