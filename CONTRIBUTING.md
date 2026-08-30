# Contributing to PacketDefinition

First off, thank you for considering contributing to PacketDefinition!

## How Can You Contribute?

### Reporting Bugs
If you find a bug, please open an issue on GitHub. Before creating a new issue, please check if the bug has already been reported. When reporting a bug, include as much detail as possible to help us reproduce it.

### Suggesting Enhancements
We welcome ideas for new features or improvements! Please open a GitHub issue to discuss your suggestions.

## Development Process

### Branching Strategy
*   **`main` branch**: Reserved for stable releases only. Do not target your PRs here.
*   **`develop` branch**: The main development branch. All contributions should be merged into this branch first.

### Pull Requests
1.  Fork the repository and create your branch from `develop`.
2.  If you've added code that should be tested, add tests.
3.  Ensure the test suite passes (see [Testing](#testing)).
4.  Make sure your code follows the project's style and documentation guidelines.
5.  Open a Pull Request against the `develop` branch.

### Documentation
*   **KDoc**: All new public methods and classes **must** include KDoc documentation.
*   **Language**: All code, comments, and documentation must be in **English**.

## Testing
Before submitting a Pull Request, please ensure that all tests pass by running the following command:

```bash
./gradlew jvmTest
```

## Commit Messages
There are no rigid requirements for commit messages, but please ensure they are descriptive and clearly explain what changes were made.

---
Thank you for your contribution!
