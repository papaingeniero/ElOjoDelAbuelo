# Hardware Preview Implementation Walkthrough (v3.3.2)

> [!NOTE]
> This legacy project now uses an **Agentic Architecture** for development rules. See `.agent/` folder.

## Changes Implemented

### 0. Documentation Refactor (`v3.3.2`)
- **Agent Rules**: Implemented strict rules for Memory/CPU constraints in `.agent/rules/legacy_dev_rules.md`.
- **Workflows**: Automated release process defined in `.agent/workflows/release_version.md`.
- **Educational Git**: Commits now require pedagogical explanations in the body.

### 1. Verification Checklist (Manual)
- [x] **Zero CPU Goal:** `SurfaceView` implementation remains stable.
- [x] **Orientation:** Landscape fixed (`v3.3.1`).
- [x] **Protocol:** Old `PROTOCOLO.md` deleted and replaced by Agent structure.

## Next Steps for User
1.  **Verify Agent Behavior**: In future sessions, the agent should automatically detect `.agent` folder rules.
2.  **Enjoy**: The codebase is now clean, documented, and ready for "Digital Nomad" workflow.
