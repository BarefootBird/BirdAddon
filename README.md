# OdinFabric External Module Example

This is an example project to demonstrate adding modules to [OdinFabric](https://github.com/odtheking/OdinFabric) using an external JAR.

## Project Setup

To set up the project, follow the Fabric documentation for configuring your development environment:  
https://docs.fabricmc.net/develop/getting-started/setting-up

### Notes While Following the Wiki

- **IntelliJ IDEA is heavily recommended.**
- When selecting a project, **clone this repository** from GitHub.
- It should automatically add the **VM arguments required for hotswapping classes and mixins**.  
  If not, add them manually as described in the wiki page.  
  *(You must use the JetBrains Runtime mentioned in the wiki for this to work.)*

## Project-Specific Notes

- In `gradle.properties`, set `odin-version` to the version you want to build upon.  
  *(Both version numbers and commit hashes are supported.)*
- Under `src/main/java`, a `mixins` directory exists — this is where all your mixins should go.
- Under `src/main/kotlin` is your main source directory, where you can add:
    - Modules
    - Other code  
