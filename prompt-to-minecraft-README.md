# Minecraftuuuum! triad

Continuuuum derivative for Minecraft mods. Three sibling repositories live **in this folder** (same split as Continuuuum / Drawer 2 / USC):

| Repo | Analog | Role |
|------|--------|------|
| [./treewriter](./treewriter) | System Drawer | Electron authoring: lemmas, scripts, three-fold isometric masks |
| [./minecraftuuuum](./minecraftuuuum) | continuuuum + runtime | Spring UCC (`:5050`), Cave/LVM adapters, NeoForge 26.1 mod |
| [./unimined-craftantic-craftpressor](./unimined-craftantic-craftpressor) | USC | SQLite schema, voxel encode, addressing, CLI |

Cursor is an **optional fee platform** only (`optionalFeePlatforms.cursor`). The `minecraftuuuum` UCC tenant auto-seeds a **Mojang/Microsoft Marketplace retainer** (creator 70% / platform 30%) plus Continuuuum’s **10% HWM**; Unity and Cursor service retainers stay off until a lobby flips to Proton Unity or Unreal. Default LLM path is Continue + local LM Studio **Codestral**.

Shared database: `minecraftuuuum.db` (craftpressor owns schema; Spring opens it; treewriter talks HTTP).

UCC is Continuuuum/USC tenant **`minecraftuuuum`** (`X-Tenant-ID`). Spring stays on **5050**. Point Continuuuum’s library at another port via `CONTINUUUUM_LIBRARY_URL` / `minecraftuuuum.continuuuum-base` if both run locally. Pact: `minecraftuuuum/pacts/minecraftuuuum-continuuuum.json` (Gradle `:spring-server:test` writes it; Drawer 2 `test_pact_minecraftuuuum_continuuuum.py` verifies Continuuuum).

## Quick start

```text
1. cd ./unimined-craftantic-craftpressor && gradlew.bat initDb
2. cd ./minecraftuuuum && gradlew.bat :spring-server:bootRun
3. cd ./treewriter && npm install && npm start
```

UCC pages: `http://127.0.0.1:5050/library`, `http://127.0.0.1:5050/lemma-library`, `http://127.0.0.1:5050/lemma-implementation`, `http://127.0.0.1:5050/block-recipes`, `http://127.0.0.1:5050/video-generation`, `http://127.0.0.1:5050/pixellight`, `http://127.0.0.1:5050/cave`. Continuuuum lobbies: `/game-lobbies` (hosting + private servers).

✓ setup a retainer the UCC tenant automatically for mojang/microsoft for 70%/30% split
✓ sql table for oauth and other connection parameters for mojang / microsoft
✓ sql table for private server information and hosting properties, connected to game sessions
✓ let's extend the continuuuum game sessions / lobbies page with hosting and property details sufficient for minecraft, and also proton unity games

✓ image mask image to model from video
✓ video animation bone alignment
✓ multi processing for video
✓ output for use in blender or 3ds max or maya
✓ video-generation web display vs Unity WebGL with Iron Man legal mode, git commit+tag tracking, and back-out
✓ screenshot isometric extrapolator with common points, 2D→3D join, micro-expression face region, stop-motion branch cache, convex tree splitter
✓ video-generation web display vs Unity WebGL with Iron Man legal mode, git commit+tag tracking, and back-out
✓ screenshot isometric extrapolator with common points, 2D→3D join, micro-expression face region, stop-motion branch cache, convex tree splitter

---
prompts:

let's make a textual lemma prompt to minecraft mod named Minecraftuuuum! Let's be derivative of continuuuum but with minecraft mods, so no retainer, and only cursor as an optional fee platform, with let's continue and local LM Studio Codestral models - let's allow built in lemmas and support for derivative minecraftuuuum mods that wrap the mod content with lemmas that make the mods useable in the script for the game like Unity asset store assets are in Continuuuum

let's execute the plan with the following repositories: treewriter (system-drawer), minecraftuuuum (continuuuum derivative), unimined-craftantic-craftpressor (USC derivative)

let's update the plan to include the new architecture, and make sure we can convert 3d assets, and images / videos to voxel encoded 3d art ready for addressing and PixelLight conversion on the web within minecraftuuuum using images and image masks, let's have a 3 fold process component for minecraft image masks per isometric dimension: image, image with mask (drawing surface & pixellight grid for brush identification (accept square as texture, as brush), and finally a PixelLight grid with defaulted pixelated version of the image as the angle side you choose (by default, expects you to complete the 6 sides of the minecraft artwork, with acute slim angle representations of the other sides between the grid representation of the current isometric side)

let's write a plan to create a new page for the UCC, /video-animation that converts the webcam animation feature of continuuuum to the UCC and allows per frame expansion of an accordian series of recorded frames from an uploaded or webcam video for human or non human ambulation for minecraft entities - let's include all official minecraft actor types and use mocrapanything or mediapipe where appropriate

let's make sure to include the full voxel ingest component per frame

let's create a page for block types and recipes, wherein, you can filter and search for any block type and get the recipies for that item + including that item by selecting the block type

let's add a duration for the video file upload to make sure we get the correct file / size / time for the video-animation - also, we're not showing the image preview for each frame, and only getting 24 frames for Downloads\6207879d.mp4 (easily 6 seconds @ 16 fps)

if i start ingesting frames of an uploaded video, it doesn't continue if i reload the page

let's make the landing page have "server configurations" each with scripts, and active servers. the server configurations should have an edit button, and the script save after edit should create a new script version that can be referenced to start a new server from a given server configuration with script

let's write a plan to update the PixelLight slides from the animation / masking with an edit button that allows you to edit using a pallet of all the active colors + primary + compliments + compliments of the active colors, in addition to a select brush that lets you select squares and see color information / highlight the selected color but not choose it

let's write a plan to add a lemma implementation page like continuuuum has, but with specific minecraft skinning and implementation features in mind

image to model page (let's use https://github.com/lightningpixel/modly to add an image to model page with optional image masks - we'll store these both on the USC server with the original image and the mask (optional uploadable), have this be a feature on the webcam-animation page, rename that "video generation" and separate model features by checkbox listbox with a container for properties to fill out for the specific model, then a Unity3d editor preview with the page in webgl mode at the bottom, let's add a Unity editor window for this, with gizmos and options for associating the bones to mesh / skin to vertex and a list of vertexes / bone, and an associated list of bounds for vertex editing, or bones / vertexes / tri - such that we can setup an image in unity or USC, sync an asset, generate / cache / upload a model, and save a BonedSkinnedAnimateableMeshRenderer that attempts to use named bounds from our loop tech, automatic bone assignment from our mediapipe or mocapanything bone tree merge, or custom animation / bone assignment. Let's create another new editor window named "Mesh animation bone assignment" that operates like the fitting wizard but creates these association objects that can freely become children of the AnimationBehaviorTree objects that rest on the player actor)

sorry, let's make sure to include custom "granularity settings" and include defaults for minecraft

let's move the three prompt-to-minecraft directories to the prompt-to-minecraft directory, from D:\Development (i guess treewriter just has to be filled in) let's implement log-view-machine with spring adapters using the stack we've setup so far

let's write a plan to implement modly image to model for system-drawer and continuuuum with a unity editor window, specifically including the VoxelRagdollActor

setup a retainer the UCC tenant automatically for mojang/microsoft for 70%/30% split
sql table for oauth and other connection parameters for mojang / microsoft
sql table for private server information and hosting properties, connected to game sessions
let's extend the continuuuum game sessions / lobbies page with hosting and property details sufficient for minecraft, and also proton unity games


