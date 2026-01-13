# GitHub Copilot Instructions

## Git Operations Policy

### ⚠️ CRITICAL: Always Require Confirmation for Git Operations

**MANDATORY RULES:**

1. **NEVER execute `git commit` without explicit user confirmation**
   - Always show the user what will be committed
   - Display file changes summary
   - Wait for explicit "yes" or confirmation before proceeding
   - If user says "no" or doesn't confirm, DO NOT commit

2. **NEVER execute `git push` without explicit user confirmation**
   - Always show what commits will be pushed
   - Display the target branch and remote
   - Show file changes in unpushed commits
   - Wait for explicit "yes" or confirmation before proceeding
   - If user says "no" or doesn't confirm, DO NOT push

3. **NEVER combine git operations without asking between each step**
   - If doing `git add && git commit && git push`, ask confirmation for EACH operation
   - Show user what's happening at each step
   - Allow user to stop the process at any point

### Required Workflow for Git Operations

#### For `git commit`:
```
1. Show staged changes: git diff --cached --stat
2. Ask: "Do you want to commit these changes with message: '<message>'? [y/N]"
3. Wait for user response
4. Only if confirmed: execute git commit
5. If not confirmed: cancel operation and inform user
```

#### For `git push`:
```
1. Show commits to push: git log --oneline @{u}..
2. Show affected files: git diff --stat @{u}..
3. Ask: "Do you want to push these commits to <remote>/<branch>? [y/N]"
4. Wait for user response
5. Only if confirmed: execute git push
6. If not confirmed: cancel operation and inform user
```

### Examples of CORRECT behavior:

**User:** "Commit the changes"
**Agent:** 
```
I see the following staged changes:
- file1.scala (modified)
- file2.scala (new)

Do you want to commit these changes with message: "Update files"? [y/N]
```
*[Wait for user response]*

**User:** "Push to GitHub"
**Agent:**
```
The following commits will be pushed to origin/main:
- abc1234 Update files
- def5678 Add new feature

Affected files:
- file1.scala
- file2.scala

Do you want to push these changes? [y/N]
```
*[Wait for user response]*

### Examples of INCORRECT behavior (DO NOT DO THIS):

❌ `git commit -m "message" && git push` without asking
❌ Executing commit/push in background without showing user
❌ Assuming user wants to proceed without explicit confirmation
❌ Using `--no-verify` flag to bypass checks

### Bypass Options

If user explicitly says:
- "force commit" or "commit without asking"
- "force push" or "push without asking"

Then you MAY skip confirmation, but:
1. Warn user about bypassing confirmation
2. Show what will be done
3. Only proceed if user acknowledges the warning

### Summary

🔒 **DEFAULT BEHAVIOR:** Always ask for confirmation before `git commit` or `git push`
⚠️ **EXCEPTION:** Only when user explicitly requests to bypass with "force" keyword
📝 **ALWAYS:** Show user what will happen before doing it

---

## Code and Documentation Language Policy

### 📝 MANDATORY: All Code and Documentation Must Be in English

**CRITICAL RULES:**

1. **All code comments MUST be in English**
   - Class and method documentation comments (docstrings)
   - Inline comments
   - TODO/FIXME/NOTE comments
   - Code annotations

2. **All documentation files MUST be in English**
   - README files (*.md)
   - Technical documentation
   - API documentation
   - Setup guides and quickstart guides
   - Security and architecture documentation

3. **Variable and function names MUST use English words**
   - Use clear, descriptive English names
   - Follow standard naming conventions
   - No transliteration from other languages

4. **When creating or modifying code:**
   - Always write comments in English
   - If you find non-English comments, translate them to English
   - Ensure consistency across the entire codebase

5. **When creating or modifying documentation:**
   - Write all content in English
   - Use proper grammar and clear technical language
   - Follow documentation best practices

### Why This Policy Exists:

- **International collaboration**: English enables developers worldwide to contribute
- **Code maintainability**: Consistent language makes code easier to understand and maintain
- **Professional standards**: English is the de facto standard for software development
- **Tool compatibility**: Most development tools and IDE features work best with English

### Examples of CORRECT behavior:

✅ `// Check if user is authenticated`
✅ `/** Validates JWT token and returns user data */`
✅ `const isValid = validatePassword(password);`

### Examples of INCORRECT behavior (DO NOT DO THIS):

❌ `// Проверяем, авторизован ли пользователь`
❌ `/** Валидирует JWT токен и возвращает данные пользователя */`
❌ `const провалид = validatePassword(пароль);`

### Enforcement:

- When generating new code: Always use English for all comments and names
- When modifying existing code: If you encounter non-English text, translate it to English
- When reviewing code: Flag any non-English content for translation

🌍 **REMEMBER:** English-only policy applies to ALL project files, including Scala, configuration, and documentation files.
