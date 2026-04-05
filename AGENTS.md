# Agent Guidelines for Consultant Backend

## Coding Rules
- Always run lint/typecheck before completing a task
- Never commit secrets or keys to the repository
- Follow existing Scala 3 code style and patterns
- Minimize output - answer concisely in 1-3 sentences

## Task Process
1. Analyze existing code before making changes
2. Create a todo list for multi-step tasks
3. Verify changes compile before finishing
4. Use existing libraries and follow conventions in the codebase

## Git Workflow
- Create feature branches for new work
- Commit frequently with descriptive messages
- Ask before making changes outside scope of request
- Never push directly to main/master

## Communication
- Be concise - avoid unnecessary preamble/explanation
- Answer directly without adding "The answer is..." or similar
- Explain non-trivial commands before running them

## Project-Specific
- This is a Scala 3 / Cats Effect / Http4s / Tapir backend
- Uses hexagonal architecture (core, data, infrastructure, api modules)
- Database: PostgreSQL with Doobie
- AWS-ready with local mock alternatives
