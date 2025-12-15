# Luna Client - Changelog

## Version 2.0.0 - Educational Release

### Major Changes
- **Rebranded from Alien to Luna Client**
  - Updated all client identifiers
  - New version numbering starting at 2.0.0
  - Updated descriptions and metadata

### Performance Improvements

#### ESP Module
- Optimized rendering loop to cache tick delta
- Improved position interpolation by extracting repeated calculations
- Refactored block entity rendering to eliminate redundant Box creation
- Added early exit checks for disabled features
- Excluded self-player from player ESP rendering

#### Combat Utilities
- **getEnemies()** - Optimized with squared distance calculations (eliminates expensive sqrt operations)
- **isValid()** - Improved early-exit logic and switched to squared distance
- **getClosestEnemy()** - Complete rewrite using more efficient distance tracking
- Cached player position to avoid multiple lookups

#### Math Utilities
- Standardized clamp() methods across all numeric types (float, double, int)
- Improved consistency using Math.max/Math.min pattern
- Added integer clamp support

#### Timer System
- Cleaned up field initialization
- Improved code readability with explicit field declarations

### Code Quality Improvements
- Better variable naming and code structure
- Reduced unnecessary object allocations
- More efficient loop iterations
- Improved null safety checks

### Documentation
- Enhanced README with feature list
- Added building instructions
- Included disclaimer for educational purposes
- Added this comprehensive changelog

### Configuration Updates
- Updated gradle.properties with new version and archive name
- Modified fabric.mod.json with Luna Client branding
- Maintained compatibility with Minecraft 1.20.4

### Educational Value
This release demonstrates several important software engineering concepts:
- Performance optimization through algorithmic improvements
- Code refactoring and maintainability
- Build system configuration
- Version control and documentation practices

## Building from Source

```bash
./gradlew clean build
```

The built JAR will be located in `build/libs/lunaclient-2.0.0.jar`

## Requirements
- Java 17 or higher
- Minecraft 1.20.4
- Fabric Loader 0.15.0 or higher

## Credits
All original authors and contributors remain credited in README.md
