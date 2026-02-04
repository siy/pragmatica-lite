# Pragmatica Lite

> **This project has moved to [pragmaticalabs/pragmatica](https://github.com/pragmaticalabs/pragmatica)**
>
> The `core/` module in the monorepo contains all Pragmatica Lite functionality (Result, Option, Promise).
>
> This repository is archived for historical reference.

## New Location

- **Repository:** https://github.com/pragmaticalabs/pragmatica
- **Module:** `core/`
- **Maven:** `org.pragmatica-lite:core`

## Why the Move?

Pragmatica Lite is now part of the Pragmatica monorepo alongside:
- `jbct/` - JBCT CLI and Maven plugin
- `aether/` - Distributed runtime  
- `integrations/` - Framework integrations (Jackson, Micrometer, JDBC, etc.)

This enables coordinated releases and simpler dependency management.

## Maven Coordinates

The Maven coordinates remain unchanged:

```xml
<dependency>
    <groupId>org.pragmatica-lite</groupId>
    <artifactId>core</artifactId>
    <version>0.11.2</version>
</dependency>
```

## Issues

Open issues have been migrated to the [monorepo issue tracker](https://github.com/pragmaticalabs/pragmatica/issues).

