# Important
- The application being developed is a Todo List App.
- Always present an action plan before starting modifications.
- **Comments & Documentation**: All comments and documentation within the code MUST be in English.
- **Notification**: Whenever you finish a task, run the `notify-send` command in the bash shell as described in the custom commands.

# Architecture
- Layers: Controller -> Service -> Repository.
- Do not inject Repositories directly into Controllers.

# Engineering Standards

## Code Style
- **Functions**: Keep between 4 and 20 lines; split if they are longer.
- **Files**: Keep under 500 lines (ideally 200-300) to avoid context loss.
- **Responsibility (SRP)**: Each function must do only one thing; each module must have a single responsibility.
- **Naming**: Use specific and unique names; avoid generic terms like `data`, `handler`, or `Manager`.
- **Searchability**: Prefer names that return fewer than 5 `grep` hits across the entire project.
- **Typing**: Mandatory explicit typing; no `any`, `Dict`, or untyped functions.
- **DRY**: Zero code duplication; extract shared logic into dedicated modules.
- **Magic Numbers**: All numeric literals must be replaced by descriptive constants to ensure clarity and maintainability.
- **Logic**: Use early returns instead of nested `if` blocks; max 2 levels of indentation.
- **Errors**: Exception messages must include the offending value and the expected state.

## Comments & Context
- **Docstrings**: Public functions must include intent and one usage example.
- **Provenance**: Reference issue numbers or commit SHAs for specific bug fixes or constraints.

## Dependencies & Structure
- **Injection**: Inject dependencies through constructors or parameters; avoid globals.
- **Abstraction**: Wrap third-party libraries behind a thin project-owned interface.
- **Formatting**: Use the language's default formatter (e.g., `prettier`, `gofmt`, `black`).

# Git Commit Best Practices

## Commit Message Structure
Follow the Conventional Commits pattern:
`type(scope): description`

[Optional Body]
[Optional Footer]

## Commit Types
- **feat**: A new feature for the user.
- **fix**: A bug fix for the user.
- **docs**: Changes to the documentation.
- **style**: Formatting, missing semi-colons, etc; no production code change.
- **refactor**: Refactoring production code, e.g. renaming a variable.
- **perf**: Code changes that improve performance.
- **test**: Adding missing tests, refactoring tests; no production code change.
- **chore**: Updating grunt tasks etc; no production code change.
- **build**: Changes that affect the build system or external dependencies.
- **ci**: Changes to CI configuration files and scripts.

## General Rules
1. **Atomic Commits**: Each commit should represent a single logical change. Do not mix multiple unrelated features or fixes in one commit.
2. **Imperative Mood**: Use the imperative mood in the subject line (e.g., "Add feature" instead of "Added feature" or "Adds feature").
3. **Subject Line Length**: Keep the first line (the summary) to 50 characters or less.
4. **Capitalization**: Start the subject line with a capital letter.
5. **No Period**: Do not end the subject line with a period.
6. **Separate Body with Blank Line**: If a body is necessary, separate it from the subject line with a blank line.
7. **Explain the "What" and "Why"**: Use the body to explain the motivation for the change and contrast it with previous behavior, rather than explaining "how" the code works.
8. **Reference Issues**: Include issue numbers or ticket IDs in the footer (e.g., `Fixes #123`).
9. **Never Commit Broken Code**: Ensure the project compiles and passes tests before committing.
10. **Avoid Sensitive Data**: Never commit secrets, API keys, or passwords. Use `.gitignore` properly.
11. **Prohibited Auto-Commits**: The AI is strictly prohibited from making any git commits unless explicitly ordered by the user. Do not perform a task and immediately commit changes without a specific request to do so.
12. **Mandatory Notification**: The AI MUST execute the `notify-send` command in the bash shell whenever any task is finished, informing the user of the completion. This is a foundational mandate and must be executed as a standalone shell command to ensure it pops up on the Linux Mint desktop.
13. **Environmental Integrity**: When running on Linux Mint, ensure `DISPLAY` and `DBUS_SESSION_BUS_ADDRESS` are respected. Never omit the notification, even if combining commands.

# Custom Model Commands
- **`srp-audit <function_name>`**: Perform a Single Responsibility Principle audit.
- **`test-gen <function_name>`**: Generate comprehensive unit tests including edge cases and mocks.
- **`dry-check`**: Scan for logic duplication and suggest abstractions.
- **`performance`**: Evaluate Big O complexity and suggest optimizations.
- **`check-standards <target>`**: Audit the specified file or code snippet against Section 2 (Engineering Standards).
- **`notify-send`**: `notify-send "Gemini-CLI" "<Message>!" -i goal-column-symbol"`
