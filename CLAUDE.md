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

### 🎯 CURRENT FOCUS: Business API Framework Development

**Immediate Commands to Run:**
```bash
# 1. Check your current status
pwd && git status && git branch --show-current

# 2. Check assigned issues
gh issue list --repo siy/team --assignee @me --state open

# 3. Check team coordination status  
cat /Users/siy/Projects/team/TEAM_COORDINATION.md

# 4. Review recent work
git log --oneline -5
```

### BUSINESS API DEVELOPMENT PRIORITIES:

**Domain Research (Priority 1):**
- Financial Services domain analysis
- E-Commerce patterns research
- Business error handling framework design

**API Design (Priority 2):**
- High-level abstraction patterns
- Type-safe business rules
- Declarative configuration systems

**Integration Planning (Priority 3):**
- Core Framework @StableAPI dependencies
- Distributed Systems coordination
- Networking layer abstractions

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
### Context Restoration Commands
```bash
# Check your context
cat /Users/siy/Projects/team/contexts/agents/business-api-agent.md

# Check team status
cat /Users/siy/Projects/team/TEAM_COORDINATION.md

# Check current git status
git status && git log --oneline -5
```

## 📋 Daily Startup Checklist

1. **Check GitHub Issues**: `gh issue list --repo siy/team --assignee @me`
2. **Check PR Status**: `gh pr list --repo siy/pragmatica-lite --author @me`
3. **Check Both Projects**: `gh issue list --repo siy/pragmatica-lite --assignee @me`  
4. **Git Status**: `pwd && git status`

## 🎯 Work Sequence Priority

1. **📋 IMMEDIATE**: Business error handling framework research and design
2. **📋 CONTINUING**: Domain pattern analysis (Financial, E-Commerce, Logistics)
3. **📋 FUTURE**: High-level API framework after Core @StableAPI ready

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

**🎯 FOCUS: Research and design business API patterns while waiting for Core Framework @StableAPI completion.**
