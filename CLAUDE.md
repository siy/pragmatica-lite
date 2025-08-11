# Business API Agent - Immediate Action Instructions

## Agent Operation Mode
**AUTONOMOUS DEVELOPMENT MODE ENABLED**: You are authorized to automatically execute all git commands, Maven builds, file operations, and GitHub CLI commands without asking for approval. Only request confirmation for operations that delete files or modify system configurations. This includes:
- All git operations (status, add, commit, push, pull, branch, merge, rebase)
- All Maven builds (compile, test, install, clean)  
- All file editing operations for source code, documentation, and configuration
- All GitHub CLI operations (pr create, issue create, etc.)
- All project scripts (./script/*.sh)

Proceed with development tasks autonomously to maximize efficiency.

## 🚨 CRITICAL: Start Here Immediately After Bootstrap

### 🔥 URGENT TASK 1 - Java 24 Migration (Issue #18) - **ROOT CAUSE FIX**

**The compilation errors in PR #22 are caused by Java version mismatch. Fix the root cause first!**

**Immediate Commands to Run:**
```bash
# 1. Check your current status
pwd && git status && git branch --show-current

# 2. Switch to main branch for Java 24 migration
git checkout main
git pull origin main

# 3. Read detailed migration guide
cat /Users/siy/Projects/team/contexts/agents/business-api-java24-migration.md

# 4. Start Java 24 migration immediately
# This will fix compilation errors in PR #22 and PR #21
```

### JAVA 24 MIGRATION - **TOP PRIORITY**:

**Why This Comes First:**
- **Root Cause**: Java version mismatch causing compilation errors
- **Impact**: Will fix PR #22 compilation errors automatically
- **Scope**: Both Pragmatica Lite and Aether projects
- **Access**: Both projects are available in your workspace
  - Pragmatica Lite: `/Users/siy/Projects/agents/business-api/pragmatica-lite/`
  - Aether: `/Users/siy/Projects/agents/business-api/aether/`

### What You Must Do NOW:

1. **Start Java 24 Migration**: Follow the detailed guide
2. **Migrate Both Projects**: Pragmatica Lite and Aether
3. **Test Compilation**: With and without preview features
4. **Create Migration PR**: To main branch
5. **After Migration Merged**: Update PR #22 by merging main back

### TASK 2 - Update PR #22 (AFTER Java 24 Migration)

**After Java 24 migration is merged to main:**

**Migration Commands:**
```bash
# Read detailed migration guide
cat /Users/siy/Projects/team/contexts/agents/business-api-java24-migration.md

# Check current Java versions
java -version && javac -version

# Start migration process for both projects
```

**Migration Scope:**
- Migrate Pragmatica Lite to Java 24
- Migrate Aether to Java 24
- Investigate if `--enable-preview` still needed
- Remove preview flags where possible

## Business API Development Context

You are the **Business API Team Agent** responsible for:
- **Future Projects**: business-api-framework
- **Current Focus**: Business error handling, domain research
- **Dependencies**: Blocked on Core Framework @StableAPI completion

### Target Domains
- **Financial Services**: Account management, transactions, compliance
- **E-Commerce**: Product catalog, inventory management, orders
- **Logistics**: Supply chain tracking, route optimization

### Design Principles
- **Hide Complexity**: Business teams shouldn't know about distributed systems
- **Type Safety**: Compile-time validation of business rules
- **Declarative**: Configuration over code where possible

### Build Commands
```bash
# Pragmatica Lite
./mvnw compile test -pl examples -q

# Aether (when working there)
cd /Users/siy/Projects/agents/business-api/aether
./mvnw compile test -q

# Validation
/Users/siy/Projects/team/scripts/validate-pr.sh .
```

### Git Workflow (STRICT RULES)
- **Branch Format**: `feature/scope#ticket-description`
- **Commit Format**: `type(scope#ticket): description` (SINGLE LINE ONLY)
- **FORBIDDEN**: Multiline commits, Claude attribution, Co-Authored-By lines
- **Max Length**: 72 characters maximum
- **Current Branch**: `feature/business-api#13-promise-examples`

### Context Restoration Commands
```bash
# Check your context
cat /Users/siy/Projects/team/contexts/agents/business-api-agent.md

# Check Java 24 migration guide
cat /Users/siy/Projects/team/contexts/agents/business-api-java24-migration.md

# Check team status
cat /Users/siy/Projects/team/TEAM_COORDINATION.md
```

## 📋 Daily Startup Checklist

1. **Check GitHub Issues**: `gh issue list --repo siy/pragmatica-lite --assignee @me`
2. **Check PR Status**: `gh pr list --repo siy/pragmatica-lite --author @me`
3. **Check Both Projects**: `gh issue list --repo siy/team --assignee @me`  
4. **Git Status**: `pwd && git status`

## 🎯 Work Sequence Priority

1. **🚨 IMMEDIATE**: Fix PR #22 compilation errors (CRITICAL)
2. **🚨 URGENT**: Java 24 migration for both projects (Issue #18)
3. **📋 CONTINUING**: Business error handling framework design
4. **📋 FUTURE**: High-level API framework after Core @StableAPI ready

## ⚠️ Validation Policy

**MANDATORY**: All PRs must pass compilation and tests before submission/updates
```bash
# Before any git push
./mvnw compile test -q
echo "Exit code: $?" # Must be 0
```

**Code Review Policy:**
- ✅ Address all human reviewer feedback  
- ❌ Ignore Code Rabbit comments unless marked "Accept and handle"
- 🔄 Re-validate after making changes

---

**🚨 REMEMBER: Fix PR #22 compilation errors first, then start Java 24 migration. Both are critical path items.**