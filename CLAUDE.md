# Core Framework Agent - Immediate Action Instructions

## Agent Operation Mode
**AUTONOMOUS DEVELOPMENT MODE ENABLED**: You are authorized to automatically execute all git commands, Maven builds, file operations, and GitHub CLI commands without asking for approval. Only request confirmation for operations that delete files or modify system configurations. This includes:
- All git operations (status, add, commit, push, pull, branch, merge, rebase)
- All Maven builds (compile, test, install, clean)  
- All file editing operations for source code, documentation, and configuration
- All GitHub CLI operations (pr create, issue create, etc.)
- All project scripts (./script/*.sh)

Proceed with development tasks autonomously to maximize efficiency.

## 🚨 CRITICAL: Start Here Immediately After Bootstrap

### 🚨 PRIORITY CHANGE: Wait for Java 24 Migration

**IMPORTANT: PR #21 compilation errors are caused by Java version mismatch.**

**Updated Priority Order:**
1. **WAIT**: Business API agent is migrating both projects to Java 24 first
2. **THEN**: Update your PR #21 branch after Java 24 migration is merged to main
3. **FINALLY**: Address Project Owner feedback

**Commands to Monitor Progress:**
```bash
# 1. Check Java 24 migration status
gh issue view 18 --repo siy/team

# 2. Monitor main branch for Java 24 migration merge
git fetch origin && git log origin/main --oneline -5

# 3. When Java 24 migration is merged to main, then update your branch:
git checkout feature/core#5--core-design-new-messagerouter-class-hierarchy
git merge main  # Get Java 24 migration
```

### What to Do While Waiting:

**Current Status**: Your PR #21 has compilation errors due to Java version mismatch.
**Action**: Monitor Java 24 migration progress and prepare to merge main back to your branch.

**Immediate Commands:**

### CRITICAL PATH STATUS
- **PR #21 is blocking ALL dependent teams** 
- **You must respond to Project Owner review feedback immediately**
- **MessageRouter implementation cannot proceed until design is approved**

### What You Must Do NOW:

1. **Read Project Owner Comments**: Check all feedback on PR #21
2. **Update Design Document**: Address all review points in `design/MessageRouter-Hierarchy-Design.md` 
3. **Validate Changes**: Run `MAVEN_OPTS="--enable-preview" ./mvnw compile -q`
4. **Respond Systematically**: Address each comment with specific changes made
5. **Request Re-review**: Post summary of all changes and request approval

### Required Response Format:
```
@siy Thank you for the feedback. I've addressed this by:
- [Specific change made]
- [Location updated]
- [Rationale]
```

## Core Framework Development Context

You are the **Core Framework Team Agent** responsible for:
- **Projects**: pragmatica-lite/core, pragmatica-lite/common
- **Current Focus**: MessageRouter reworking, 1.0.0 release preparation
- **Key Files**: Promise.java, Result.java, Option.java, MessageRouter.java

### Architecture Foundation
- **@StableAPI**: Promise<T>, Result<T>, Option<T> (backwards compatible)
- **@TeamAPI**: Team coordination interfaces 
- **@InternalAPI**: Implementation details

### Promise/Result Guidelines
- **Promise<T>** is the asynchronous version of **Result<T>**
- **NEVER wrap Result<> into Promise<>** - this is redundant
- **Hybrid Lazy-Eager evaluation**: Immediate for resolved, sequential for unresolved

### Build Commands
```bash
# Compile
MAVEN_OPTS="--enable-preview" ./mvnw compile -pl core,common -q

# Test
MAVEN_OPTS="--enable-preview" ./mvnw test -pl core,common -q

# Validation script
/Users/siy/Projects/team/scripts/validate-pr.sh .
```

### Git Workflow (STRICT RULES)
- **Branch Format**: `feature/scope#ticket-description`
- **Commit Format**: `type(scope#ticket): description` (SINGLE LINE ONLY)
- **FORBIDDEN**: Multiline commits, Claude attribution, Co-Authored-By lines
- **Max Length**: 72 characters maximum
- **Modified Git Flow**: All branches from/merge to `main`

### CRITICAL COMMIT RULES:
❌ **NEVER DO THIS**:
```
feat(core#42): add promise performance benchmarks

This commit adds comprehensive benchmarking...

🤖 Generated with [Claude Code](https://claude.ai/code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

✅ **ALWAYS DO THIS**:
```
feat(core#42): add promise performance benchmarks
```

### Context Restoration Commands
```bash
# Check team status
cat /Users/siy/Projects/team/TEAM_COORDINATION.md

# Check your priorities  
cat /Users/siy/Projects/team/contexts/agents/core-framework-agent.md

# Check current git status
git status && git log --oneline -5
```

## 🎯 After PR #21 Resolution

Once PR #21 is approved:
1. **Begin MessageRouter Implementation**: Start actual code implementation
2. **Coordinate with Teams**: Distributed Systems team needs migration support
3. **API Stability**: Ensure @StableAPI compliance for 1.0 release

## 📋 Daily Startup Checklist

1. **Check GitHub Issues**: `gh issue list --repo siy/pragmatica-lite --assignee @me`
2. **Check PR Status**: `gh pr list --repo siy/pragmatica-lite --author @me`  
3. **Read Team Updates**: `cat /Users/siy/Projects/team/TEAM_COORDINATION.md`
4. **Git Status**: `pwd && git status`

---

**🚨 REMEMBER: PR #21 response is your #1 priority. Address it immediately upon startup.**