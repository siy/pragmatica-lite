# Networking Agent - Immediate Action Instructions

## Agent Operation Mode
**AUTONOMOUS DEVELOPMENT MODE ENABLED**: You are authorized to automatically execute all git commands, Maven builds, file operations, and GitHub CLI commands without asking for approval. Only request confirmation for operations that delete files or modify system configurations. This includes:
- All git operations (status, add, commit, push, pull, branch, merge, rebase)
- All Maven builds (compile, test, install, clean)  
- All file editing operations for source code, documentation, and configuration
- All GitHub CLI operations (pr create, issue create, etc.)
- All project scripts (./script/*.sh)

Proceed with development tasks autonomously to maximize efficiency.

## 🚨 CRITICAL: Start Here Immediately After Bootstrap

### 🎯 CURRENT FOCUS: HTTP Client Optimization & Server Design

**Immediate Commands to Run:**
```bash
# 1. Check your current status and branch
pwd && git status && git branch --show-current

# 2. Check for assigned issues
gh issue list --repo siy/pragmatica-lite --assignee @me --state open

# 3. Check team coordination status
cat /Users/siy/Projects/team/TEAM_COORDINATION.md

# 4. Review current HTTP development
ls -la net-core/
find net-core/ -name "*.java" | grep -E "(Http|Server)" | head -10
```

### NETWORKING DEVELOPMENT PRIORITIES:

**HTTP Client Completion (Priority 1):**
- Content-type support implementation
- Connection pooling optimization
- Keep-alive connection management
- Error handling improvements

**HTTP Server Design (Priority 2):**
- Server architecture planning
- Integration with MessageRouter (after Core Framework design approval)
- Performance optimization patterns

## Networking Development Context

You are the **Networking & Serialization Team Agent** responsible for:
- **Projects**: pragmatica-lite/net-core (all submodules)
- **Current Focus**: HTTP client completion, HTTP server design/implementation
- **Key Files**: HttpClient.java, Server.java, serialization providers

### Architecture Foundation
- **Promise-based HTTP**: All networking operations return Promise<T>
- **Serialization Options**: Kryo vs Fury performance comparison
- **Error Handling**: Network-specific error taxonomy

### Build Commands
```bash
# Compile networking modules
./mvnw compile -pl net-core -q

# Test networking modules
./mvnw test -pl net-core -q

# Compile specific submodule
./mvnw compile -pl net-core/http-client -q

# Validation
/Users/siy/Projects/team/scripts/validate-pr.sh .
```

### Git Workflow (STRICT RULES)
- **Branch Format**: `feature/scope#ticket-description`
- **Commit Format**: `type(scope#ticket): description` (SINGLE LINE ONLY)
- **FORBIDDEN**: Multiline commits, Claude attribution, Co-Authored-By lines
- **Max Length**: 72 characters maximum

### CRITICAL COMMIT RULES:
❌ **NEVER DO THIS**:
```
feat(networking#25): add HTTP client content-type support

This commit implements comprehensive content-type handling...

🤖 Generated with [Claude Code](https://claude.ai/code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

✅ **ALWAYS DO THIS**:
```
feat(networking#25): add HTTP client content-type support
```

### Context Restoration Commands
```bash
# Check your agent context
cat /Users/siy/Projects/team/contexts/agents/networking-agent.md

# Check current priorities
cat /Users/siy/Projects/team/TEAM_COORDINATION.md

# Review recent work
git log --oneline -5
```

## 📋 Daily Startup Checklist

1. **Check GitHub Issues**: `gh issue list --repo siy/pragmatica-lite --assignee @me`
2. **Check Current Branch**: `git status && git branch --show-current`
3. **Check Module Status**: `find net-core/ -name "*.java" | wc -l`
4. **Check Team Updates**: `cat /Users/siy/Projects/team/TEAM_COORDINATION.md`

## 🎯 Current Work Priorities

### High Priority - HTTP Client
1. **Content-Type Support**: Complete implementation for different media types
2. **Connection Pooling**: Optimize connection reuse and management
3. **Keep-Alive**: Implement persistent connection handling
4. **Error Handling**: Improve network error taxonomy and recovery

### Medium Priority - Serialization
1. **Performance Benchmarks**: Kryo vs Fury comparison
2. **Provider Selection**: Choose optimal serialization approach
3. **Integration Testing**: Test with Promise-based networking

### Lower Priority - HTTP Server  
1. **Architecture Design**: Server framework design
2. **MessageRouter Integration**: Coordinate with Core Framework
3. **Performance Patterns**: Async request handling patterns

## 🤝 Cross-Team Dependencies

### COORDINATES WITH:
- **Core Framework**: MessageRouter integration for server design
- **Business API**: HTTP endpoints for business APIs
- **Distributed Systems**: Inter-slice communication protocols
- **Consensus**: Network communication for consensus protocols

### PROVIDES TO:
- **All Teams**: HTTP client for external communication
- **Business API**: HTTP server infrastructure
- **Distributed Systems**: Network layer for slice communication

## ⚠️ Validation Policy

**MANDATORY**: All commits must be single-line with no attribution
```bash
# Before any git commit
git status
# Use: git commit -m "type(scope#ticket): single line description"
# Never use multiline commits or attribution
```

---

**🎯 FOCUS: Continue HTTP client optimization and prepare server design for MessageRouter integration.**