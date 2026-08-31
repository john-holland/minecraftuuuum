# Minecraftuuuum!

Continuuuum derivative: textual lemmas `{P:lemma|key=value}` bind to Minecraft registry IDs. **UCC** is this repo’s web library (Spring on port **5050**).

- `lemma-core` — URN, `{P:}` / `{M:}` parsers, builtin vocabulary
- `spring-server` — thesaurus, lemma-build (LM Studio Codestral), voxel/PixelLight HTTP, `/video-generation`, Cave/LVM adapters
- `neoforge-mod` — Minecraft 26.1 NeoForge runtime

Sibling of [treewriter](../treewriter) and [unimined-craftantic-craftpressor](../unimined-craftantic-craftpressor) under `prompt-to-minecraft`. The triad README (prompts, layout, retainer notes) is [prompt-to-minecraft-README.md](./prompt-to-minecraft-README.md); the parent folder `README.md` is a symlink to that file.

Tenant snail **`minecraftuuuum`** (`X-Tenant-ID`). Spring UCC stays on **5050**. Set `CONTINUUUUM_LIBRARY_URL` if Continuuuum Flask also wants 5050.

## Run UCC

```text
gradlew.bat :spring-server:bootRun
```

- http://127.0.0.1:5050/library
- http://127.0.0.1:5050/video-generation
- http://127.0.0.1:5050/lemma-library
- http://127.0.0.1:5050/cave (`POST /cave/route`)

Set `CRAFTPRESSOR_DB` (default `./minecraftuuuum.db`) and `LM_STUDIO_BASE` (default `http://localhost:1234/v1`).

## Pact (UCC → Continuuuum)

`ContinuuuumTenantClient` is the consumer of Continuuuum Flask (`library/search`, payroll split, oauth connections). Consumer tests write `pacts/minecraftuuuum-continuuuum.json`. Continuuuum verifies that file from Drawer 2 `Scripts/tests/test_pact_minecraftuuuum_continuuuum.py`.

```text
gradlew.bat :spring-server:test --tests com.minecraftuuuum.server.ContinuuuumPactConsumerTest
```
