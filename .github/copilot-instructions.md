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
