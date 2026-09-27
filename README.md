# OpenSpeakers

## Change the mod version

The mod version can be updated in all required files at once with a Gradle
task.

```bash
./gradlew setVersion '-PnewVersion=1.1.0'
```

The task updates the version in:

- `build.gradle`
- `src/main/java/com/marcusnebel/openspeakers/OpenSpeakers.java`
- `src/main/resources/mcmod.info`

## Source installation

This code follows the Minecraft Forge installation method. It applies small
patches to the vanilla MCP source code, providing access to the data and
functions required to build the mod.

The patches are based on the unrenamed MCP source code, also known as
`srgnames`. They cannot be applied directly to normally named source code.

### Prerequisites

See the [Forge documentation](http://mcforge.readthedocs.io/en/latest/gettingstarted/)
for more information about setting up the development environment.

### Eclipse

1. Open a command line and change to the project directory.
2. Run the following command:

   ```bash
   ./gradlew genEclipseRuns
   ```

   On Windows, use `gradlew genEclipseRuns` instead.
3. Open Eclipse and select **Import > Existing Gradle Project**.
4. Select the project directory, or run `gradlew eclipse` to generate the
   project.

#### Known Eclipse issue

1. Open **Project > Run/Debug Settings**.
2. Edit `runClient` and `runServer` under **Environment**.
3. Set `MOD_CLASSES` to contain `[modid]%%[Path]` twice instead of the four
   automatically generated entries.

### IntelliJ IDEA

1. Open IntelliJ IDEA and import the project.
2. Select `build.gradle` and import it.
3. Run the following command:

   ```bash
   ./gradlew genIntellijRuns
   ```

4. Refresh the Gradle project in IntelliJ IDEA if necessary.

### Useful Gradle commands

If libraries are missing from the IDE, refresh the dependencies:

```bash
./gradlew --refresh-dependencies
```

Use `clean` to reset the build state without changing the source code:

```bash
./gradlew clean
```

## Help

If the setup still does not work:

- Ask for help in the ForgeGradle channel on EsperNet.
- Contact the Forge project community on [Discord](https://discord.gg/UvedJ9m).
- See the [Forge forum](http://www.minecraftforge.net/forum/index.php/topic,14048.0.html)
   for more information.

## Additional information

- [LexManos' installation video](https://www.youtube.com/watch?v=8VEdtQLuLO0&feature=youtu.be)
- MinecraftForge ships with this source code and installs it as part of the
   Forge installation process.