### Requirements
- Java 25
- Minecraft 26.1-26.2
- Fabric loader 0.19.3
- NeoForge 26.1-26.2

### Changes (0.1.9)
- Added search matching for hangul typed with the ime off, with a config toggle (ekdl → 다이아)
- Added korean search to the game rule filter
- Improved creative search by reusing item names instead of rebuilding them on every keystroke
- Fixed hangul detection treating other scripts as hangul, and the search toggle not applying until the query changed